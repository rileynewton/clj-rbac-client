(ns puppetlabs.rbac-client.testutils.dummy-rbac-service
  (:require [puppetlabs.rbac-client.protocols.rbac :refer [RbacConsumerService]]
            [puppetlabs.trapperkeeper.services :refer  [defservice]]
            [slingshot.slingshot :refer  [throw+]]
            [puppetlabs.rbac-client.services.rbac :refer [str->uuid]]))

(def dummy-rbac (reify RbacConsumerService
                  (is-permitted? [this subject perm-str] true)
                  (are-permitted? [this subject perm-strs] true)
                  (cert-whitelisted? [this ssl-client-cn] true)
                  (valid-token->subject [this jwt-str]
                    (if (or (not jwt-str) (= "invalid-token" jwt-str))
                      (throw+ {:kind :puppetlabs.rbac/invalid-token
                               :msg (format "Token: %s" jwt-str)})
                      {:login "test_user"
                       :id (str->uuid "751a8f7e-b53a-4ccd-9f4f-e93db6aa38ec")}))))

(defservice dummy-rbac-service
  RbacConsumerService
  []
  (is-permitted? [this subject perm-str] true)
  (are-permitted? [this subject perm-strs] true)
  (cert-whitelisted? [this ssl-client-cn] true)
  (valid-token->subject [this jwt-str]
    (if (or (not jwt-str) (= "invalid-token" jwt-str))
      (throw+ {:kind :puppetlabs.rbac/invalid-token
               :msg (format "Token: %s" jwt-str)})
      {:login "test_user"
       :id (str->uuid "751a8f7e-b53a-4ccd-9f4f-e93db6aa38ec")})))
