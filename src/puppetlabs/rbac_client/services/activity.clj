(ns puppetlabs.rbac-client.services.activity
  (:require
   [puppetlabs.rbac-client.protocols.activity :refer [ActivityReportingService]]
   [puppetlabs.http.client.common :as http]
   [puppetlabs.http.client.sync :refer [create-client]]
   [puppetlabs.rbac-client.core :refer [json-api-caller]]
   [puppetlabs.trapperkeeper.core :refer [defservice]]
   [puppetlabs.trapperkeeper.services :refer [service-context]]))

(defservice remote-activity-reporter
  "service to report to a remote activity service"
  ActivityReportingService
  [[:ConfigService get-in-config]]
  (init [this context]
    (let [api-url (get-in-config [:activity-consumer :api-url])
          ssl-config (get-in-config [:global :certs])
          route-limit {:max-connections-per-route 20}
          client (create-client (merge route-limit ssl-config))]
      (assoc context :client client
             :activity-client (partial json-api-caller client api-url))))

  (stop [this context]
    (if-let [client (-> this service-context :client)]
      (http/close client))
    context)

  (report-activity! [this event-bundle]
    (let [activity-client (-> this service-context :activity-client)]
      (activity-client :post "/v1/events" {:body event-bundle}))))
