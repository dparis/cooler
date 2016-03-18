(ns workbench
  (:require [clojure.pprint :refer [pprint]]
            [cooler.core :as c]
            [cooler.rate-limiter.core :as crl]
            [cooler.store.core :as cst]
            [cooler.store.redis :as csr]
            [eftest.runner :as eft]
            [repl]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [java.lang.management ManagementFactory]))


;;; Enable schema validation for all functions while in dev

(s/set-fn-validation! true)


;;;; Vars


;;;; Functions

(defn show-jvm-opts
  "Returns an array of option strings passed to the current
  JVM running the app."
  []
  (let [runtime-mx-bean (ManagementFactory/getRuntimeMXBean)
        jvm-args        (.getInputArguments runtime-mx-bean)]
    jvm-args))

(defn rerun-tests
  ([]
   (rerun-tests "test"))
  ([path]
   (repl/reset)
   (eft/run-tests (eft/find-tests path)
                  {:report eftest.report.pretty/report})))

(defn test-redis-rate-limit
  []
  (let [conn-opts {:pool {} :spec {}}
        key-fn    (fn [k] (str "cooler:rate-limiter-test:" k))
        store     (csr/make-redis-store conn-opts key-fn)
        rl        (c/rate-limiter store 30 :minute 0)]
    (crl/rate-limit! rl "test-limit" 1)))
