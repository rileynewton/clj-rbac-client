### 1.1.4
 * add optional identity provider id into RBAC subject
 * update clj-parent to 5.2.11

### 1.1.3 
 * update str->uuid function to allow UUIDs

### 1.1.2
 * update clj-parent 4.6.17

### 1.1.1
 * fix activity reporting event handling

### 1.1.0
  * deprecate cert-whitelisted?
  * convert client to use RBAC v2/certs endpoint in favor of v1.  Falls back to v1 if v2 isn't available
  * use clj-parent 4.6.3
  
### 1.0.0
  * update to use clj-parent 2.6.0
  * add support for activity v2 api and fall back to v1 if v2 isn't supported

### 0.9.4
  * Add wrap-cert-only-access authentication middleware for endpoints that only accept
    RBAC whitelisted SSL certs.

### 0.9.1
### 0.8.3
  * Add to rbac client protocol the "subject" interface, that given a user
  id, will return the subject that corresponds to that id.

### 0.9.0
### 0.8.2
  * Implement interface for list-permitted-for method in RBAC

### 0.6.1
  * Update to clj-parent 0.4.2 to pickup i18n 0.7.0 for pot file renaming

### 0.6.0
  * Allow tokens to be passed to `valid-token->subject` with `|no_keepalive` suffix to avoid
    updating `last_active` time.

### 0.5.5
  * fix dummy service to return uuids instead of strings
  
### 0.5.4
  * update to clj-parent 0.2.5 to use clj-http 0.7.0 for i18n support
  
### 0.5.3
  * use clj-parent 0.2.4 for dependency versioning
  
### 0.5.2
  * Incorporates a bug fix from the 0.4.2 release
  
### 0.5.1
  * Update the url for the v2 /auth/token/authenticate endpoint to be the correct one
  
### 0.5.0
  * Update valid-token->subject to query the v2 token/authenticate endpoint, to
    enable updates to the last_active timestamp for timeout-based token
    invalidation.
    
### 0.4.2
  * Update subject processing to convert group-ids into uuids to match schema validation expectations
  
### 0.4.1
  * Change description of valid-token->subject to describe the new RBAC token implementation behavior
  * filter the output of valid-token->subject to make sure behavior agrees with local consumer
  
### 0.4.0
  * Throw an exception in init of remote-rbac-consumer-service if rbac consumer has no api-url.
  * Ensure that are-permitted? always returns a vector.
