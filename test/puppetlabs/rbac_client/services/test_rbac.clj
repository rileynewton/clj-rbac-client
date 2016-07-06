(ns puppetlabs.rbac-client.services.test-rbac
  (:require [clojure.test :refer [are deftest is testing]]
            [puppetlabs.http.client.sync :refer [create-client]]
            [puppetlabs.kitchensink.json :as json]
            [puppetlabs.rbac-client.protocols.rbac :as rbac]
            [puppetlabs.rbac-client.services.rbac :refer [remote-rbac-consumer-service api-url->status-url]]
            [puppetlabs.rbac-client.testutils.config :as cfg]
            [puppetlabs.rbac-client.testutils.http :as http]
            [puppetlabs.trapperkeeper.app :as tk-app]
            [puppetlabs.trapperkeeper.testutils.bootstrap :refer [with-app-with-config]]
            [puppetlabs.trapperkeeper.testutils.webserver :refer [with-test-webserver-and-config]])
  (:import [java.util UUID]))

(def ^:private rand-subject {:login "rando@whoknows.net", :id (UUID/randomUUID)})

(def ^:private configs
  (let [server-cfg (cfg/jetty-ssl-config)]
    {:server server-cfg
     :client (cfg/rbac-client-config server-cfg)}))

(defn- wrap-test-handler-middleware
  [handler]
  (http/wrap-test-handler-middleware handler (:client configs)))

(deftest test-is-permitted?
  (testing "is-permitted? returns the first result from RBAC's API"
    (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
      (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)]
        (doseq [result [[true] [false]]]
          (let [handler (wrap-test-handler-middleware
                          (constantly (http/json-200-resp result)))]
            (with-test-webserver-and-config handler _ (:server configs)
              (is (= (first result)
                     (rbac/is-permitted? consumer-svc rand-subject "users:disable:1"))))))))))

(deftest test-are-permitted?
  (testing "are-permitted? passes through the result from RBAC's API"
    (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
      (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)]
        (dotimes [_ 10]
          (let [result (vec (repeatedly (rand-int 30) #(< (rand) 0.5)))
                handler (wrap-test-handler-middleware
                          (constantly (http/json-200-resp result)))]
            (with-test-webserver-and-config handler _ (:server configs)
              (is (= result
                     (rbac/are-permitted? consumer-svc rand-subject ["users:disable:1"]))))))))))

(deftest test-cert-whitelisted?
  (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)]
      (doseq [result [true false]]
        (let [handler (wrap-test-handler-middleware
                        (constantly (http/json-200-resp {:cn "foobar", :whitelisted result})))]
          (with-test-webserver-and-config handler _ (:server configs)
            (is (= result (rbac/cert-whitelisted? consumer-svc "foobar")))))))))

(deftest test-valid-token->subject
  (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)
          handler (wrap-test-handler-middleware
                    (constantly (http/json-200-resp rand-subject)))]

      (with-test-webserver-and-config handler _ (:server configs)
        (is (= rand-subject (rbac/valid-token->subject consumer-svc "token")))))))

(deftest test-status-url
  (are [service-url rbac-api-url] (= service-url (api-url->status-url rbac-api-url))
    "https://foo.com:4444/status/v1/services"
    "https://foo.com:4444/rbac/rbac-api"

    "http://foo.com:4444/status/v1/services"
    "http://foo.com:4444/rbac/rbac-api"

    "https://foo.com/status/v1/services"
    "https://foo.com/rbac/rbac-api"

    "http://foo.com/status/v1/services"
    "http://foo.com/rbac/rbac-api"))

(deftest test-status-check-without-config
  (with-app-with-config tk-app [remote-rbac-consumer-service]
    (assoc-in (:client configs) [:rbac-consumer :api-url] nil)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)
          handler (wrap-test-handler-middleware
                   (fn [req]
                     (http/json-200-resp {:foo :bar})))]
      (with-test-webserver-and-config handler _ (:server configs)
        (is (= :unknown (:state (rbac/status consumer-svc "critical"))))))))

(deftest test-status-check
  (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)
          status-results {:activity-service
                          {:service_version "0.5.3",
                           :service_status_version 1,
                           :detail_level "info",
                           :state "running",
                           :status {:db_up true}}

                          :rbac-service
                          {:service_version "1.2.12",
                           :service_status_version 1,
                           :detail_level "info",
                           :state "running",
                           :status {:db_up true,
                                    :activity_up true}}}

          failed-response (http/malformed-json-400-resp "No 'level' parameter found in request")
          handler (wrap-test-handler-middleware
                   (fn [req]
                     (if (= "critical" (get-in req [:params "level"]))
                       (http/json-200-resp status-results)
                       failed-response)))
          error-handler (wrap-test-handler-middleware
                         (fn [req]
                           (if (= "critical" (get-in req [:params "level"]))
                             (http/json-200-resp
                              (-> status-results
                                  (assoc-in [:rbac-service :state] "error")
                                  (assoc-in [:rbac-service :status :db_up] false)))
                             failed-response)))]

      (with-test-webserver-and-config handler _ (:server configs)
        (is (= {:service_version "1.2.12",
                :service_status_version 1,
                :detail_level "info",
                :state :running,
                :status {:db_up true,
                         :activity_up true}}
               (rbac/status consumer-svc "critical"))))

      (with-test-webserver-and-config error-handler _ (:server configs)
        (is (= {:service_version "1.2.12",
                :service_status_version 1,
                :detail_level "info",
                :state :error,
                :status {:db_up false,
                         :activity_up true}}
               (rbac/status consumer-svc "critical")))))))
