(ns cooler.schemas.rate-limit-result
  "Schema which validates the result of calling rate-limit on
  an object which implements the RateLimiter protocol."
  (:require [schema.core :as s]))


(s/defschema RateLimitResult
  {:limited?        s/Bool
   :limit           s/Int
   :remaining       s/Int
   :reset-after-mus s/Int
   :retry-after-mus (s/maybe s/Int)})
