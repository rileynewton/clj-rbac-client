(ns puppetlabs.rbac-client.testutils.config
  (:import [java.net ConnectException Socket]))

(defn random-port
  "Returns a random port number in the 'ephemeral port range' of 49152 - 65535.
  Attempts to return an open port, but this cannot be guaranteed because another
  process may have bound to the port after the port was checked."
  []
  (let [port (+ 49152 (rand-int (- 65536 49152)))
        open? (try
                (-> (Socket. "localhost" port) .close)
                false
                (catch ConnectException _
                  true))]
    (if open?
      port
      (recur))))

(def client-ssl-config
  "A puppetlabs.http.client SSL configuration with a client certificate,
  referencing the SSL files in the `dev-resources/ssl` directory."
  {:ssl-ca-cert "dev-resources/ssl/ca.pem"
   :ssl-cert "dev-resources/ssl/cert.pem"
   :ssl-key "dev-resources/ssl/key.pem"})

(defn jetty-ssl-config
  "Return a TK Webserver Jetty9 config that defines an SSL server running on
  `localhost` on a random port, and uses the SSL files in the
  `dev-resources/ssl` directory. Note that while an effort is made to choose an
  open port, the port cannot be guaranteed to be open."
  []
  (merge client-ssl-config
         {:ssl-host "localhost"
          :ssl-port (random-port)
          :client-auth "need"}))

(defn rbac-client-config
  "Given a Jetty SSL config map, return a config map for use with TK testutils'
  `with-app-with-config` that contains configuration for the remote
  implementation of the RBAC Consumer Service to connect to the SSL server
  specified in the Jetty config."
  [jetty-ssl-config]
  (let [{:keys [ssl-host ssl-port]} jetty-ssl-config]
    {:rbac-consumer {:api-url (format "https://%s:%s/rbac-api" ssl-host ssl-port)}
     :activity-consumer {:api-url (format "https://%s:%s/activity-api" ssl-host ssl-port)}
     :global {:certs client-ssl-config
              :logging-config "dev-resources/logback-test.xml"}}))
