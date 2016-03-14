(ns cooler.schemas.rate-unit
  "Schema which validates a rate limit quota unit."
  (:require [cooler.util :as u]
            [schema.core :as s]))

(s/defschema RateUnit
  "Schema which validates a rate limit quota unit."
  (apply s/enum u/mus-conversion-units))
