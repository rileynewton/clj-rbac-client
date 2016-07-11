(ns puppetlabs.rbac-client.middleware.test-authentication
  (:require [clojure.test :refer [are deftest is testing]]
            [cheshire.core :as json]
            [puppetlabs.rbac-client.middleware.authentication :as middleware]
            [puppetlabs.rbac-client.protocols.rbac :as rbac]
            [puppetlabs.rbac-client.services.rbac :refer [remote-rbac-consumer-service]]
            [puppetlabs.rbac-client.testutils.config :as cfg]
            [puppetlabs.rbac-client.testutils.http :as http]
            [puppetlabs.ssl-utils.core :as ssl-utils]
            [puppetlabs.trapperkeeper.app :as tk-app]
            [puppetlabs.trapperkeeper.testutils.bootstrap :refer [with-app-with-config]]
            [puppetlabs.trapperkeeper.testutils.webserver :refer [with-test-webserver-and-config]])
  (:import [java.util UUID]))

(def ^:private rand-subject {:login "rando@whoknows.net", :id (UUID/randomUUID)})

(def ^:private configs
  (let [server-cfg (cfg/jetty-ssl-config)]
    {:server server-cfg
     :client (cfg/rbac-client-config server-cfg)}))

(def test-request
  {:server-port (get-in configs [:server :ssl-port])
   :server-name (get-in configs [:server :ssl-host])
   :remote-addr "127.0.0.1"
   :uri "/secret-stuff"
   :scheme :https
   :request-method :get
   :protocol "HTTP/1.1"})

(def request-with-token
  (assoc test-request :headers {@#'puppetlabs.rbac-client.middleware.authentication/authn-header
                                "totes_legit"}))

(def request-with-cert
  (assoc test-request :ssl-client-cert "foo.example.com"))

(deftest token-auth
  (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)]
      (testing "when everything goes well, the subject map is added to the request object"
        (let [rbac-handler (constantly (http/json-200-resp rand-subject))]
          (with-test-webserver-and-config rbac-handler _ (:server configs)
            (let [authd-handler (->> (fn [req] (is (= rand-subject (:subject req))))
                                     (middleware/wrap-token-only-access consumer-svc))]
              (authd-handler request-with-token)))))

      (testing "the middleware blocks requests that"
        (testing "are missing a token"
          (let [authd-handler (->> (fn [req] (is (true? "middleware blocked request")))
                                   (middleware/wrap-token-only-access consumer-svc))
                resp (authd-handler (assoc request-with-token :headers {}))]
            (is (= 401 (:status resp)))))

        (testing "have an invalid token"
          (let [rbac-handler (constantly
                               (http/json-resp 401 {:kind :puppetlabs.rbac/invalid-token}))]
            (with-test-webserver-and-config rbac-handler _ (:server configs)
              (let [authd-handler (->> (fn [req] (is (true? "middleware blocked request")))
                                       (middleware/wrap-token-only-access consumer-svc))
                    resp (authd-handler request-with-token)]
                (is (= 401 (:status resp)))))))))))

(deftest token-and-cert-auth
  (with-app-with-config tk-app [remote-rbac-consumer-service] (:client configs)
    (let [consumer-svc (tk-app/get-service tk-app :RbacConsumerService)]
      (testing "when everything goes well, the subject map is added to the request object"
        (let [rbac-handler (constantly (http/json-200-resp {:whitelisted true :subject rand-subject})) ]
          (with-test-webserver-and-config rbac-handler _ (:server configs)
            (with-redefs [ssl-utils/get-cn-from-x509-certificate (constantly "foo.example.com")]
              (let [authd-handler (->> (fn [req] (http/json-200-resp (:subject req)))
                                       (middleware/wrap-token-and-cert-access consumer-svc))
                    resp (authd-handler request-with-cert)]
                (is (= 200 (:status resp)))
                (is (= (update rand-subject :id str)
                       (json/parse-string (:body resp) true))))))))
      (testing "the middleware blocks requests that have a non-whitelisted cert"
        (let [rbac-handler (constantly (http/json-200-resp {:whitelisted false :subject nil}))]
          (with-test-webserver-and-config rbac-handler _ (:server configs)
            (with-redefs [ssl-utils/get-cn-from-x509-certificate (constantly "foo.example.com")]
              (let [authd-handler (->> (fn [req] (is (true? "middleware blocked request")))
                                       (middleware/wrap-token-and-cert-access consumer-svc))
                    resp (authd-handler request-with-cert)]
                (is (= 401 (:status resp)))))))))))


