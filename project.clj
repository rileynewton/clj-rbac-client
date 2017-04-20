(defn deploy-info
  [url]
  {:url url
   :username :env/nexus_jenkins_username
   :password :env/nexus_jenkins_password
   :sign-releases false})

(defproject puppetlabs/rbac-client "0.7.1-SNAPSHOT"
  :description "Tools for interacting with PE RBAC"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :parent-project {:coords [puppetlabs/clj-parent "0.6.1"]
                   :inherit [:managed-dependencies]}

  :dependencies [[org.clojure/clojure]
                 [ring/ring-core]
                 [ring/ring-json]
                 [puppetlabs/ring-middleware]
                 [slingshot]
                 [puppetlabs/kitchensink]
                 [puppetlabs/http-client]
                 [puppetlabs/trapperkeeper]
                 [puppetlabs/i18n]]

  :pedantic? :abort
  :profiles {:dev {:dependencies [[puppetlabs/kitchensink :classifier "test"]
                                  [puppetlabs/trapperkeeper :classifier "test"]
                                  [puppetlabs/trapperkeeper-webserver-jetty9]
                                  [puppetlabs/trapperkeeper-webserver-jetty9 :classifier "test"]]}
             :testutils {:source-paths ^:replace  ["test"]}}

  :plugins [[lein-parent "0.3.1"]
            [puppetlabs/i18n "0.8.0"]]

  :classifiers  [["test" :testutils]]

  :test-paths ["test"]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]])
