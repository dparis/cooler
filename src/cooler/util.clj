(ns cooler.util
  "Utility functions used by cooler."
  (:require [clojure.set :as c-set]
            [schema.core :as s]))


(s/defn contains-keys? :- s/Bool
  "Does map m contain all keys ks?"
  [m  :- {s/Any s/Any}
   ks :- [s/Any]]
  (if (or (empty? ks) (nil? ks))
    false
    (c-set/subset? (set ks) (set (keys m)))))

(def ^:private mus-unit-conversions
  {:millisecond 1000
   :second      (* 1000 1000)
   :minute      (* 60 1000 1000)
   :hour        (* 60 60 1000 1000)
   :day         (* 24 60 60 1000 1000)})

(def mus-conversion-units
  "Valid microsecond units of conversion."
  (set (keys mus-unit-conversions)))

(s/defn mus->ms :- s/Int
  "Returns the given microsecond value mus as millisecond value."
  [mus :- s/Int]
  (long (/ mus 1000)))

(s/defn period-mus :- s/Int
  "Returns the interval period of time in microseconds for a rate of
  amount per unit."
  [amount :- s/Int
   unit   :- (apply s/enum mus-conversion-units)]
  (/ (get mus-unit-conversions unit) amount))
