(ns puppetlabs.pe-clients.test-server
  (:require [ring.middleware.params :as ring-params]
            [ring.middleware.json :refer [wrap-json-body
                                          wrap-json-response]]
            [puppetlabs.trapperkeeper.core :as tk]))


(defn make-json-handler
  [response]
  (let [handler (fn handler
                  [request]
                  (assoc-in response [:body :_request] request
                            [:headers]  {"Content-Type" "application/json"}))]
    (-> handler
        wrap-json-response
        (wrap-json-body {:keywords? true}))))

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
