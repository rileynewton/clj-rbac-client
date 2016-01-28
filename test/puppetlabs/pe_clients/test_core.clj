(ns puppetlabs.pe-clients.test-core
  (:require [clojure.test :refer [deftest testing is]]
           [puppetlabs.pe-clients.core :as core]
           [puppetlabs.pe-clients.test-server :as test-server]))

(deftest test-api-caller
  (is (= 0 0)))
