(ns cooler.core
  "Clojure rate-limiting library."
  (:require [cooler.rate-limiter.core :as rl]
            [cooler.rate-limiter.gcra :as rlg]
            [cooler.schemas.rate-unit
             :refer [RateUnit]]
            [cooler.store.core :as st]
            [cooler.store.redis :as sr]
            [schema.core :as s]))


(s/defn rate-limiter :- (s/protocol rl/RateLimiter)
  "Returns a RateLimiter backed by the provided store, which limits requests
  based on a quota defined as amount per unit. Optionally, a burst value
  can be provided which specifies the number of requests allowed to exceed the
  rate in a single burst."
  ([store amount unit]
   (rate-limiter store amount unit 0 :gcra))
  ([store amount unit burst]
   (rate-limiter store amount unit burst :gcra))
  ([store        :- (s/protocol st/RateLimitStore)
    amount       :- (s/constrained s/Int #(>= % 0))
    unit         :- RateUnit
    burst        :- (s/constrained s/Int #(>= % 0))
    limiter-type :- (s/enum :gcra)]
   (case limiter-type
     :gcra (rlg/make-gcra-rate-limiter store amount unit burst))))
