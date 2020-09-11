(ns puppetlabs.rbac-client.protocols.rbac)

(defprotocol RbacConsumerService
  (is-permitted? [this subject perm-str]
    "Given an RbacSubject map and permission string of the form
    \"object_type:action:instance\", returns true or false.")

  (are-permitted? [this subject perm-strs]
    "Given an RbacSubject map and a list of permission strings of the form
    \"object_type:action:instance\", returns a corresponding vector of boolean
    responses.")

  (cert-whitelisted? [this ssl-client-cn]
    "DEPRECATED: use cert-allowed? instead. Given an SSL client CN string, returns whether or not that CN is on RBAC's
    certificate whitelist.")

  (cert-allowed? [this ssl-client-cn]
    "Given an SSL client CN string, returns whether or not that CN is on RBAC's
    certificate whitelist.")

  (cert->subject [this ssl-client-cn]
    "Given an SSL client CN string, returns the subject associated with that
    CN, or nil if no subject is associated.")

  (valid-token->subject [this token-str]
    "Given a token as a string, this function:

    1) Checks the validity of the token with RBAC
    2) Converts the token into the subject it represents
    3) Returns the subject.

    If 1, 2 fails, a slingshot exception describing the failure is
    thrown.")

  (status [this level]
    "Returns the TK status map at the supplied `level`. The `state`
    key has the most interesting information. Callers should not rely
    on the contents of `:status`, which contains the details used to
    compute the `:state` value.")

  (list-permitted [this token object-type action]
    "Returns the list of instances that correspond to the users permissions for
    a given `object-type` and `action` pair. ")

  (list-permitted-for [this subject object-type action]
    "Returns the list of instances that correspond to the the user associated with
    the provided subject, for the given `object_type` and `action` pair.")

  (subject [this uuid]
   "Given a UUID for a given user, return the full subject representation for the user"))
