(ns puppetlabs.pe-clients.protocols.activity)

(defprotocol ActivityConsumerService
  "This has a single report function for now. This protocol is very simple and should be considered disposable!"
  (report! [this body]))
