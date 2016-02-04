(def tk-jetty-version "1.5.0")
(def tk-version "1.2.0")

(defn deploy-info
  [url]
  {:url url
   :username :env/nexus_jenkins_username
   :password :env/nexus_jenkins_password
   :sign-releases false})

(defproject puppetlabs/rbac-client "0.1.1-SNAPSHOT"
  :description "Tools for interacting with PE RBAC"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-json  "0.3.1" :exclusions [clj-time]]
                 [slingshot "0.12.2"]
                 [puppetlabs/kitchensink "1.2.0" :exclusions [joda-time clj-time]]
                 [puppetlabs/http-client "0.5.0"]
                 [puppetlabs/trapperkeeper ~tk-version :exclusions [joda-time clj-time]]]
  :pedantic? :abort
  :profiles {:test {:dependencies [
                                   [puppetlabs/trapperkeeper ~tk-version :classifier "test" :exclusions [joda-time clj-time]]
                                   [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version :exclusions  [joda-time clj-time]]
                                   [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version :classifier "test" :exclusions  [joda-time clj-time]]]}
             :testutils {:source-paths ^:replace  ["test"]}}

  :classifiers  [["test" :testutils]]

  :test-paths ["test"]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]]
  )
