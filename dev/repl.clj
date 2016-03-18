(ns repl
  (:require [clojure.pprint :as pprint]
            [clojure.tools.namespace.repl :refer [refresh]]))


;;; Basic system life cycle

(def system nil)

(defn init [])

(defn start [])

(defn stop [])

(defn go
  []
  (init)
  (start))

(defn reset
  []
  (stop)
  (refresh :after 'repl/go))
