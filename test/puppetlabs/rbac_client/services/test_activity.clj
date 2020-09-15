(ns puppetlabs.rbac-client.services.test-activity
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest testing is]]
            [puppetlabs.rbac-client.protocols.activity :as act]
            [puppetlabs.rbac-client.services.activity :refer [remote-activity-reporter]]
            [puppetlabs.rbac-client.testutils.config :as cfg]
            [puppetlabs.rbac-client.testutils.http :as http]
            [puppetlabs.trapperkeeper.app :as tk-app]
            [puppetlabs.trapperkeeper.testutils.bootstrap :refer [with-app-with-config]]
            [puppetlabs.trapperkeeper.testutils.webserver :refer [with-test-webserver-and-config]]))

(def ^:private configs
  (let [server-cfg (cfg/jetty-ssl-config)]
    {:server server-cfg
     :client (cfg/rbac-client-config server-cfg)}))

(defn- wrap-test-handler-middleware
  [handler]
  (http/wrap-test-handler-mw handler))

(def v2-bundle
  {:commit {:service {:id "namer"}
            :subject {:name "Herman Melville"
                      :id "42bf351c-f9ec-40af-84ad-e976fec7f4bd"}
            :objects 
              [{:name "Herman Aldrich"
                :id "2d5d4e0b-5353-4bbd-9c4c-084177caac32"
                :type "a Herman"}
               {:name "Herman Miller",
                :id "2d5d4e0b-5353-4bbd-9c4c-084177caac33",
                :type "a Herman"}
               {:name "Herman Stump",
                :id "2d5d4e0b-5353-4bbd-9c4c-084177caac34",
                :type "a Herman"}
               {:name "Herman W. Hellman",
                :id "2d5d4e0b-5353-4bbd-9c4c-084177caac35",
                :type "a Herman"}]
            :ip-address "an ip address"}})

(def v1-bundle
  {:commit {:service {:id "namer"}
            :subject {:name "Herman Melville" 
                      :id "42bf351c-f9ec-40af-84ad-e976fec7f4bd"}
            :object {:name "Herman Aldrich" 
                     :id "2d5d4e0b-5353-4bbd-9c4c-084177caac32" 
                     :type "a Herman"}}})

(def expected-v1-bundle
  {:commit {:service {:id "namer"}
            :subject {:name "Herman Melville"
                      :id "42bf351c-f9ec-40af-84ad-e976fec7f4bd"}
            :object {:name "Herman Aldrich"
                     :id "2d5d4e0b-5353-4bbd-9c4c-084177caac32"
                     :type "a Herman"}}})

(def expected-v1-upgraded-bundle
  {:commit {:service {:id "namer"}
            :subject {:name "Herman Melville"
                      :id "42bf351c-f9ec-40af-84ad-e976fec7f4bd"}
            :objects [{:name "Herman Aldrich"
                       :id "2d5d4e0b-5353-4bbd-9c4c-084177caac32"
                       :type "a Herman"}]}})


(deftest test-activity
  (with-app-with-config tk-app [remote-activity-reporter] (:client configs)
      (let [consumer-svc (tk-app/get-service tk-app :ActivityReportingService)
            handler (wrap-test-handler-middleware
                             (fn [req]
                               (if (= "/activity-api/v2/events" (:uri req))
                                 (let [parsed-body (json/parse-string (:body req) true)]
                                   (is (or (= v2-bundle parsed-body) (= expected-v1-upgraded-bundle parsed-body)))
                                   ;; provide a payload for testability.  Actual service doesn't guarantee providing return results
                                   (http/json-200-resp {:success true :actual (:body req)}))
                                 (is false))))
            v1-handler (wrap-test-handler-middleware
                        (fn [req]
                          (case (:uri req)
                            "/activity-api/v2/events" (http/json-resp 404 {:success false})
                            "/activity-api/v1/events" (http/json-200-resp {:success true :actual (:body req)}))))]

        (testing "v2 endpoint supported with passthrough"
          (with-test-webserver-and-config handler _ (:server configs)
            (let [result (act/report-activity! consumer-svc v2-bundle)]
              (is (= 200 (:status result)))
              (is (= true (get-in result [:body :success])))
              (is (= v2-bundle (json/parse-string (get-in result [:body :actual]) true))))))

        (testing "upgrades to v2 payload when v1 payload is submitted"
          (with-test-webserver-and-config handler _ (:server configs)
                                          (let [result (act/report-activity! consumer-svc v1-bundle)]
                                            (is (= 200 (:status result)))
                                            (is (= true (get-in result [:body :success])))
                                            (is (= expected-v1-upgraded-bundle (json/parse-string (get-in result [:body :actual]) true))))))

        ;; note that after this test is done, if any more tests are added,
        ;; the client will have stored that the v2 endpoint isn't available internally, and
        ;; will use the v1 endpoint.  If this is undesirable, a new "with-app-with-config" should be created.
        (testing "downgrades to v1 endpoint when v2 is a 404"
          (with-test-webserver-and-config v1-handler _ (:server configs)
            (let [result (act/report-activity! consumer-svc v2-bundle)]
              (is (= 200 (:status result)))
              (is (= true (get-in result [:body :success])))
              (is (= expected-v1-bundle (json/parse-string (get-in result [:body :actual]) true)))))))))