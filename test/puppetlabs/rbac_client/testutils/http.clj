(ns puppetlabs.rbac-client.testutils.http
  (:require [puppetlabs.kitchensink.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [slingshot.slingshot :refer [throw+]]
            [puppetlabs.rbac-client.services.rbac :refer [api-url->status-url]])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(defn wrap-read-body
  "Middleware to convert the :body field of the ring request map into a string."
  [handler]
  (fn [req]
    (handler (update req :body slurp))))

(def ^:private no-json-400-resp
  {:status 400
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string
           {:kind "not-json-content-type"
            :msg (str "Request has a body but the 'Content-Type' header does"
                      " not include 'application/json")})})

(defn- malformed-json-400-resp
  [parse-error]
  {:status 400
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string
           {:kind "malformed-request"
            :msg "The request's body is not valid JSON."
            :details {:error (str parse-error)}})})

(defn wrap-check-json
  "Wrap a test ring handler with middleware that returns an error to the client if:
    * the body is non-empty and the Content-Type header does not contain
      \"application/json\".
    * the body is non-empty and the Content-Type header contains
      \"application/json\" but the body is not parseable JSON.
  "
  [handler]
  (fn [req]
    (if (empty? (:body req))
      (handler req)
      (if-not (re-find #"application/json" (:content-type req))
        no-json-400-resp
        (try
          (json/parse-string (:body req))
          (handler req)
          (catch JsonParseException e
            (malformed-json-400-resp e)))))))

(defn wrap-check-base-url
  "Given a handler and the TK config map for the consumer service, wrap the
  handler with middleware that tests that the request's URL contains the RBAC
  API URL set for the RBAC remote consumer service in the TK config map at the
  path [:rbac-consumer :api-url]. If not, throws a slingshot exception."
  [handler config]
  (let [api-url (get-in config [:rbac-consumer :api-url])]
    (when-not (string? api-url)
      (throw (IllegalArgumentException.
               "config map does not contain an RBAC consumer API URL")))
    (fn [req]
      (let [req-url (format "https://%s:%d%s" (:server-name req) (:server-port req) (:uri req))]
        (when-not (or (.contains req-url api-url)
                      (.contains req-url (api-url->status-url api-url)))
          (throw+ {:kind ::mismatched-urls
                   :msg (format "The request's url '%s' does not contain the RBAC API URL '%s'"
                                req-url api-url)
                   :details {:request-url req-url
                             :rbac-url api-url}}))
        (handler req)))))

(defn wrap-test-handler-middleware
  "Given a handler and the TK config map for the consumer service, wrap the
  handler with test middlewares that assert the handler is being given
  well-formed JSON requests to the same server as specified in the config."
  [handler config]
  (-> handler
    wrap-params
    wrap-check-json
    wrap-read-body
    (wrap-check-base-url config) ))

(defn json-resp
  "Construct a ring response map with the given status code that has an
  'application/json' Content Type and `body-val` serialized to JSON for its
  body."
  [status body-val]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string body-val)})

(defn json-200-resp
  "Construct a ring response map that's a 200 OK response with an
  'application/json' Content Type and has `body-val` serialized to JSON for its
  body."
  [body-val]
  (json-resp 200 body-val))
