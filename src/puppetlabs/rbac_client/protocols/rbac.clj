(ns puppetlabs.rbac-client.protocols.rbac)

(defprotocol RbacConsumerService
  (is-permitted? [this subject perm-str]
    "Given an RbacSubject map and permission string of the form
    \"object_type:action:instance\", returns true or false.")

  (are-permitted? [this subject perm-strs]
    "Given an RbacSubject map and a list of permission strings of the form
    \"object_type:action:instance\", returns a corresponding list of boolean
    responses.")

  (cert-whitelisted? [this ssl-client-cn]
    "Given an ssh client CN string, returns whether or not that CN is on RBAC's
    certificate whitelist.")

  (valid-token->subject [this jwt-str]
    "Given a JWT string, this function:

    1) Checks validity of JWT using the pubkey specified in the rbac-cfg
    2) Checks to see if token has expired
    3) Converts the token into the subject it represents
    4) Checks to see if the subject is revoked.
    5) Returns the subject.

    If 1, 2 or 4 fail, a slingshot exception describing the failure is
    thrown."))
