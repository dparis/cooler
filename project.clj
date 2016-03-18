(defproject cooler "0.1.0-SNAPSHOT"
  :description "A distributed rate-limiting library based on the GCRA algorithm."
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url  "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.5.0"

  :dependencies [[org.clojure/clojure "1.7.0"]

                 ;; NOTE: Override [2.32.0] included by carmine to
                 ;;       prevent conflict with timbre
                 ;;       -- DP 2016-03-11
                 [com.taoensso/encore "2.36.2"]

                 ;; NOTE: Override [1.0.0] included by carmine to
                 ;;       prevent conflict with timbre
                 ;;       -- DP 2016-03-11
                 [com.taoensso/truss "1.1.1"]

                 [com.taoensso/carmine "2.12.2"]
                 [clj-time "0.11.0"]
                 [funcool/cuerdas "0.7.0"]
                 [prismatic/schema "1.0.5"]
                 [com.taoensso/timbre "4.3.1"]]

  :jvm-opts ["-Duser.timezone=UTC"]

  :profiles {:dev {:source-paths ["dev"]

                   :repl-options {:init-ns workbench
                                  :init    (repl/init)}

                   :dependencies [[eftest "0.1.0"]]

                   :plugins      [[codox "0.9.4"]
                                  [jonase/eastwood "0.2.3"]
                                  [lein-ancient "0.6.7"]]}}

  :aliases {"start-dev-server" ["trampoline" "with-profile" "dev" "run" "--config" "dev-resources/config.edn"]
            "lint"             ["eastwood" "{:exclude-namespaces [:test-paths]}"]})
