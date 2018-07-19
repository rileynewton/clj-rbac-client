(ns puppetlabs.rbac-client.test-core
  (:require [clojure.test :refer [deftest testing is]]
            [puppetlabs.rbac-client.core :as core]
            [puppetlabs.trapperkeeper.testutils.webserver :refer [with-test-webserver]]
            [puppetlabs.trapperkeeper.testutils.logging :refer [with-test-logging]]
            [puppetlabs.http.client.sync :refer [create-client]]
            [puppetlabs.rbac-client.test-server :as test-server]
            [slingshot.test]))

(deftest test-api-caller
  (with-test-logging
    (let [client (create-client {})
          app (constantly {:status 200 :body "ok"})
          port 38924]
      (with-test-webserver app port
        (is (= "ok"
               (:body (core/api-caller client (format "http://localhost:%s/" port) :get ""))))))

    (let [client (create-client {})
          app (constantly {:status 500 :body "server error"})
          port 38924]
      (with-test-webserver app port
        (is (= "server error" (:body (core/api-caller client (format "http://localhost:%s/" port) :get ""))))
        (is (thrown+? [:kind :puppetlabs.rbac-client/status-error]
                      (:body (core/api-caller client (format "http://localhost:%s/" port) :get "" {:status-errors true}))))))

    (let [client (create-client {})
          app (constantly {:status 500 :body "ok"})
          port 38924]
      (is (thrown+? [:kind :puppetlabs.rbac-client/connection-failure]
                    (:body (core/api-caller client (format "http://localhost:%s/" port) :get "")))))))

(deftest test-json-api-caller
  (with-test-logging
    (let [client (create-client {})
          app (test-server/make-json-handler {:status 200
                                              :body {:foo 1 :bar {:baz 2}}})
          port 38924]
      (with-test-webserver app port
        (let [response (core/json-api-caller client (format "http://localhost:%s/foo" port) :get "/bar?p=1")]
          (is (= 200 (:status response)))
          (is (= 1 (get-in response [:body :foo])))
          (is (= "application/json" (get-in response [:body :_request :headers :accept])))
          (is (= "/foo/bar" (get-in response [:body :_request :uri])))
          (is (= "1" (get-in response [:body :_request :params :p]))))))

    (let [client (create-client {})
          app (test-server/make-json-handler {:status 400
                                              :body {:kind :invalid
                                                     :msg "oops"}})
          port 38924]
      (with-test-webserver app port
        (is (thrown+? [:kind :invalid]
                      (core/json-api-caller client (format "http://localhost:%s/" port) :get "" {:throw-body true})))
        (is (thrown+? [:kind :puppetlabs.rbac-client/status-error]
                      (core/json-api-caller client (format "http://localhost:%s/" port) :get "" {:status-errors true})))))))
