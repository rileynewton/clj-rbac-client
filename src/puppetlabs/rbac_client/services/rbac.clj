(ns puppetlabs.rbac-client.services.rbac
  (:require
   [clojure.string]
   [clojure.tools.logging :as log]
   [puppetlabs.i18n.core :as i18n]
   [puppetlabs.http.client.common :as http]
   [puppetlabs.http.client.sync :refer [create-client]]
   [puppetlabs.rbac-client.core :refer [json-api-caller http-error?, parse-body]]
   [puppetlabs.rbac-client.protocols.rbac :refer [RbacConsumerService]]
   [puppetlabs.trapperkeeper.core :refer [defservice]]
   [puppetlabs.trapperkeeper.services :refer [service-context]]
   [slingshot.slingshot :refer [throw+]])
  (:import [java.util UUID]
           [java.net URI]))

(def ^:private ^:const authn-header "x-authentication")

(defn perm-str->map
  "Given a permission string of the form <object_type>:<action>:(<instance>|*),
  return a map that represents the permission.
  COPY-PASTE: https://github.com/puppetlabs/pe-rbac-service/blob/master/src/clj/puppetlabs/rbac/services/consumer.clj"
  [perm-str]
  (let [[object-type action instance] (clojure.string/split perm-str #":" 3)]
    {:object_type object-type
     :action action
     :instance instance}))

(defn str->uuid
  "Convert a string into a UUID. If the ojbect is already a UUID return it"
  [str-uuid]
  (try
    (if (uuid? str-uuid) str-uuid (UUID/fromString str-uuid))
    (catch IllegalArgumentException _
      (throw+ {:uuid str-uuid
               :msg (i18n/tru "Error parsing UUID {0}" str-uuid)
               :kind ::invalid-uuid}))))

(defn group-ids-str->uuids
  [subject-map]
  (if-let [group-ids (:group_ids subject-map)]
    (assoc subject-map :group_ids (map str->uuid group-ids))
    subject-map))

(defn parse-subject
  [subject]
  (if subject
    (-> subject
        (select-keys [:id :login :display_name :email
                      :last_login :role_ids :inherited_role_ids
                      :group_ids :is_superuser :is_revoked
                      :is_remote :is_group])
        group-ids-str->uuids
        (update :id str->uuid))))

(defn rbac-client
  "Wrap the json caller adding :throw-body to opts"
  ([client rbac-url method path] (rbac-client client rbac-url method path {}))
  ([client rbac-url method path opts]
   ;; use merge to allow caller to override "throw-body"
   (json-api-caller client rbac-url method path (merge {:throw-body true} opts))))

(defn api-url->status-url
  "Given an RBAC api-url, convert that to the related status URL"
  [^String api-url]
  (let [^URI api-url (java.net.URI. api-url)]
    (str (.getScheme api-url)
         "://"
         (.getHost api-url)
         (when-not (= -1 (.getPort api-url))
           (str ":" (.getPort api-url)))
         "/status/v1/services")))

(defn request-certs-output
  ([prefix client ssl-client-cn]
   (request-certs-output prefix client ssl-client-cn true))
  ([prefix client ssl-client-cn throw-error?]
   (let [url (format "/%s/certs/%s" prefix ssl-client-cn)]
     (-> (client :get url {:throw-body throw-error?})))))

(defn cert-allowed?
  [context ssl-client-cn]
  (when ssl-client-cn
    (let [v1-certs? (deref (:v1-certs? context))
          try-v2? (or (nil? v1-certs?) (false? v1-certs?))
          {:keys [rbac-client]} context]
      (if try-v2?
        ;; don't throw on failures to allow us to detect the 404
        (let [v2-response (request-certs-output "v2" rbac-client ssl-client-cn false)
              status (:status v2-response)]
          (if (= 404 status)
            (do
              (swap! (:v1-certs? context) (constantly true))
              (get-in (request-certs-output "v1" rbac-client ssl-client-cn)
                      [:body :whitelisted]))
            ;; if we got an error, make sure we handle it the way the rbac-client does
            (if (http-error? v2-response)
              (throw+ (update-in (parse-body v2-response) [:kind] keyword))
              (get-in v2-response [:body :allowlisted]))))
        (get-in (request-certs-output "v1" rbac-client ssl-client-cn)
                [:body :whitelisted])))))

(defservice remote-rbac-consumer-service
  RbacConsumerService
  [[:ConfigService get-in-config]]

  (init [_ tk-ctx]
    (if-let [rbac-url (get-in-config [:rbac-consumer :api-url])]
      (let [ssl-config (get-in-config [:global :certs])
            route-limit {:max-connections-per-route 20}
            certified-client (create-client (merge route-limit ssl-config))
            uncertified-client (create-client (merge route-limit (select-keys ssl-config [:ssl-ca-cert])))]
        (assoc tk-ctx
               :client certified-client
               :uncertified-client uncertified-client
               :rbac-client (partial rbac-client certified-client rbac-url)
               :uncertified-rbac-client (partial rbac-client uncertified-client rbac-url)
               :status-client (partial rbac-client certified-client (api-url->status-url rbac-url))
               :v1-certs? (atom nil)))
      (throw+ {:kind :puppetlabs.rbac-client/invalid-configuration
               :message (i18n/tru "''rbac-consumer'' not configured with an ''api-url''")})))

  (stop [_ tk-ctx]
    (when-let [client (:client tk-ctx)]
      (http/close client))
    (when-let [client (:uncertified-client tk-ctx)]
      (http/close client))
    (dissoc tk-ctx :client :uncertified-client))

  (is-permitted? [this subject perm-str]
    (let [{:keys [rbac-client]} (service-context this)
          body {:token (str (:id subject))
                :permissions [(perm-str->map perm-str)]}]
      (-> (rbac-client :post "/v1/permitted" {:body body})
          :body
          first)))

  (are-permitted? [this subject perm-strs]
    (let [{:keys [rbac-client]} (service-context this)
          body {:token (str (:id subject))
                :permissions (map perm-str->map perm-strs)}]
      (-> (rbac-client :post "/v1/permitted" {:body body})
          :body)))

  (cert-whitelisted? [this ssl-client-cn]
    (log/warn (i18n/trs "DEPRECATION: the 'cert-whitelisted?' function has been deprecated and will be removed in future versions. Use `cert-allowed?` instead."))
    (cert-allowed? (service-context this) ssl-client-cn))

  (cert-allowed? [this ssl-client-cn]
    (cert-allowed? (service-context this) ssl-client-cn))

  (cert->subject [this ssl-client-cn]
    (when ssl-client-cn
      (let [context (service-context this)
            v1-certs? (deref (:v1-certs? context))
            try-v2? (or (nil? v1-certs?) (false? v1-certs?))
            {:keys [rbac-client]} context]
        (if try-v2?
          ;; don't throw on errors so we can detect the 404
          (let [v2-response (request-certs-output "v2" rbac-client ssl-client-cn false)
                status (:status v2-response)]
            (if (= 404 status)
              (do
                (swap! (:v1-certs? context) (constantly true))
                (-> (request-certs-output "v1" rbac-client ssl-client-cn)
                    (get-in [:body :subject])
                    parse-subject))
              (if (http-error? v2-response)
                (throw+ (update-in (parse-body v2-response) [:kind] keyword))
                (parse-subject (get-in v2-response [:body :subject])))))
          (parse-subject (get-in (request-certs-output "v1" rbac-client ssl-client-cn)
                               [:body :subject]))))))

  (valid-token->subject [this token-str]
    (let [{:keys [rbac-client]} (service-context this)
          [with-suffix? actual-token _] (re-matches #"(.*)\|no_keepalive" token-str)
          payload {:token (if with-suffix? actual-token token-str)
                   :update_last_activity? (not with-suffix?)}]
      (-> (rbac-client :post "/v2/auth/token/authenticate" {:body payload})
          :body
          (parse-subject))))

  (status [this level]
    (let [{:keys [status-client]} (service-context this)]
      (-> (status-client :get "" {:query-params {"level" (name level)}})
          (get-in [:body :rbac-service])
          (update :state keyword))))

  (list-permitted [this token object-type action]
    (let [{:keys [uncertified-rbac-client]} (service-context this)
          headers {:headers {authn-header token}}]
      (-> (uncertified-rbac-client :get (str "/v1/permitted/" object-type "/" action) headers)
          :body)))

  (list-permitted-for [this subject object-type action]
    (let [{:keys [rbac-client]} (service-context this)
          user-id (str (:id subject))]
      (-> (rbac-client :get (str "/v1/permitted/" object-type "/" action "/" user-id))
          :body)))

  (subject [this user-id]
    (let [{:keys [rbac-client]} (service-context this)]
      (-> (rbac-client :get (str "/v1/users/" user-id))
          :body
          parse-subject))))
