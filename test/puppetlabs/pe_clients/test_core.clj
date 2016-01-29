(ns puppetlabs.pe-clients.test-core
  (:require [clojure.test :refer [deftest testing is]]
            [puppetlabs.pe-clients.core :as core]
            [puppetlabs.trapperkeeper.testutils.webserver :refer [with-test-webserver]]
            [puppetlabs.trapperkeeper.testutils.logging :refer [with-test-logging]]
            [puppetlabs.http.client.sync :refer [create-client]]
            [puppetlabs.pe-clients.test-server :as test-server]
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
          app (constantly {:status 500 :body "ok"})
          port 38924]
      (with-test-webserver app port
        (is (thrown+? [:kind :puppetlabs.pe-clients/rest-failure]
               (:body (core/api-caller client (format "http://localhost:%s/" port) :get ""))))))
(let [client (create-client {})
          app (constantly {:status 500 :body "ok"})
          port 38924]
        (is (thrown+? [:kind :puppetlabs.pe-clients/connection-failure]
               (:body (core/api-caller client (format "http://localhost:%s/" port) :get "")))))
    ))
