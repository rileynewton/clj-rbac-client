(ns puppetlabs.rbac-client.services.activity
  (:require
   [clojure.tools.logging :as log]
   [puppetlabs.rbac-client.protocols.activity :refer [ActivityReportingService]]
   [puppetlabs.http.client.common :as http]
   [puppetlabs.http.client.sync :refer [create-client]]
   [puppetlabs.rbac-client.core :refer [json-api-caller]]
   [puppetlabs.trapperkeeper.core :refer [defservice]]
   [puppetlabs.trapperkeeper.services :refer [service-context]]))

(defn v1->v2
  ;; make any adjustments needed to make a (maybe) v1 payload into a v2 payload
  [event-bundle]
  (if (contains? event-bundle :object)
    (-> event-bundle
        (dissoc :object)
        (assoc :objects [(:object event-bundle)]))
    event-bundle))

(defn v2->v1
  ;; make any adjustments needed to make a (maybe) v2 payload into a v1 payload
  [event-bundle]
  (let [result (dissoc event-bundle :ip-address)]
    (if (contains? event-bundle :objects)
      (-> (dissoc result :objects)
          (assoc :object (first (:objects event-bundle))))
      result)))

(defn report-activity
  [context event-bundle]
  (let [activity-client (:activity-client context)
        supports-v2? (:supports-v2-api context)]
    (if @supports-v2?
      (let [v2-bundle (v1->v2 event-bundle)
            result (activity-client :post "/v2/events" {:body v2-bundle})]
        ;; if we get a 404, the activity service doesn't support the v2 endpoint, so
        ;; cache that, and retry.
        (if (= 404 (:status result))
          (do
            (log/info "Configured activity service does not support v2 API, falling back to v1")
            (swap! supports-v2? (constantly false))
            (report-activity context event-bundle))
          result))
      (let [v1-bundle (v2->v1 event-bundle)]
       (activity-client :post "/v1/events" {:body v1-bundle})))))

(defservice remote-activity-reporter
  "service to report to a remote activity service"
  ActivityReportingService
  [[:ConfigService get-in-config]]
  (init [this context]
    (let [api-url (get-in-config [:activity-consumer :api-url])
          ssl-config (get-in-config [:global :certs])
          route-limit {:max-connections-per-route 20}
          client (create-client (merge route-limit ssl-config))]
      (assoc context
             :client client
             :supports-v2-api (atom true)
             :activity-client (partial json-api-caller client api-url))))

  (stop [this context]
    (if-let [client (-> this service-context :client)]
      (http/close client))
    context)

  (report-activity! [this event-bundle]
    (let [context (service-context this)]
      (report-activity context event-bundle))))