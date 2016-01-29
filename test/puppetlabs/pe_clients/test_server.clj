(ns puppetlabs.pe-clients.test-server
  (:require
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.json :refer [wrap-json-body
                                 wrap-json-response]]
   [puppetlabs.trapperkeeper.core :as tk]))


(defn make-json-handler
  [response]
  (let [handler (fn handler
                  [request]
                  (-> response
                      (assoc-in [:body :_request] (dissoc request :body))
                      (assoc :headers {"Content-Type" "application/json"})))]
    (-> handler
        wrap-json-response
        (wrap-json-body {:keywords? true})
        wrap-keyword-params
        wrap-params)))

(defn non-json-error-handler
  [req]
  ({:status 404
    :body "Not Found"}))

(defn build-test-service
  [handler]
  (tk/service
   [[:WebserverService add-ring-handler]]
   (init [this context]
         (add-ring-handler handler)
         context)))
