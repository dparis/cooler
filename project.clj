(defproject cooler "0.1.1-SNAPSHOT"
  :description "A distributed rate-limiting library based on the GCRA algorithm."
  :url "https://github.com/dparis/cooler"
  :license {:name "The MIT Licence"
            :url  "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.6.1"

  :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; Explicit includes to prevent confusing dependencies
                 [com.taoensso/encore "2.88.2"]

                 [com.taoensso/carmine "2.15.1"]
                 [clj-time "0.13.0"]
                 [funcool/cuerdas "2.0.2"]
                 [prismatic/schema "1.1.3"]
                 [com.taoensso/timbre "4.8.0"]]

  :deploy-repositories [["releases" :clojars]]

  :jvm-opts ["-Duser.timezone=UTC"]

  :profiles {:dev {:source-paths ["dev"]

                   :repl-options {:init-ns workbench
                                  :init    (repl/init)}

                   :dependencies [[eftest "0.1.2"]]

                   :plugins      [[codox "0.10.2"]
                                  [jonase/eastwood "0.2.3"]
                                  [lein-ancient "0.6.10"]]}}

  :aliases {"lint" ["eastwood" "{:exclude-namespaces [:test-paths]}"]})
