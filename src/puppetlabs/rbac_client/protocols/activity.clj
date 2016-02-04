(ns puppetlabs.rbac-client.protocols.activity)

(defprotocol ActivityReportingService
  "This has a single report function for now. This protocol is very simple and should be considered disposable!"
  (report-activity! [this event-bundle]))
