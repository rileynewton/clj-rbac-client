(ns puppetlabs.pe-clients.testutils.dummy-activity-service
  (:require [puppetlabs.pe-clients.protocols.activity :refer [ActivityReportingService]]
            [puppetlabs.trapperkeeper.services :refer  [defservice]]))

(def dummy-activity (reify ActivityReportingService
                      (report-activity! [this body] nil)))

(defservice dummy-activity-service
  ActivityReportingService
  []
  (report-activity! [this body] nil))
