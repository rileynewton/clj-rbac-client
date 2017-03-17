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
