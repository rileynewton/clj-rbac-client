(def tk-jetty-version "1.5.0")
(def tk-version "1.2.0")

(defproject puppetlabs/pe-clients "0.1.0-SNAPSHOT"
  :description "Clients for pe services"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-json  "0.3.1" :exclusions [clj-time]]
                 [slingshot "0.12.2"]
                 [puppetlabs/kitchensink "1.2.0" :exclusions [joda-time clj-time]]
                 [puppetlabs/http-client "0.4.7-SNAPSHOT"]
                 [puppetlabs/trapperkeeper ~tk-version :exclusions [joda-time clj-time]]]
  :pedantic? :abort
  :profiles {:test {:dependencies [
                                   [puppetlabs/trapperkeeper ~tk-version :classifier "test" :exclusions [joda-time clj-time]]
                                   [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version :exclusions  [joda-time clj-time]]
                                   [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version :classifier "test" :exclusions  [joda-time clj-time]]]}
             :testutils {:source-paths ^:replace  ["test/clj"]}}

  :classifiers  [["test" :testutils]]

  :test-paths ["test"])
