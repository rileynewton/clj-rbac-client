(ns puppetlabs.rbac-client.services.test-rbac
  (:require [clojure.test :refer [are deftest is testing]]
            [puppetlabs.http.client.sync :refer [create-client]]
            [puppetlabs.kitchensink.json :as json]
            [puppetlabs.rbac-client.protocols.rbac :as rbac]
            [puppetlabs.rbac-client.services.rbac :refer [remote-rbac-consumer-service]]
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
