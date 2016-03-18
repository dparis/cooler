(ns cooler.schemas.redis.connection-options
  "Schema which validates a Redis ConnectionOptions map."
  (:require [cooler.util :as u]
            [schema.core :as s]))


(s/defschema ^:private PoolConfig
  {(s/optional-key :min-idle-per-key)                s/Int
   (s/optional-key :max-idle-per-key)                s/Int
   (s/optional-key :max-total-per-key)               s/Int

   (s/optional-key :block-when-exhausted?)           s/Int
   (s/optional-key :lifo?)                           s/Bool
   (s/optional-key :max-total)                       s/Int
   (s/optional-key :max-wait-ms)                     s/Int
   (s/optional-key :min-evictable-idle-time-ms)      s/Int
   (s/optional-key :num-tests-per-eviction-run)      s/Int
   (s/optional-key :soft-min-evictable-idle-time-ms) s/Int
   (s/optional-key :swallowed-exception-listener)    s/Int
   (s/optional-key :test-on-borrow?)                 s/Bool
   (s/optional-key :test-on-return?)                 s/Bool
   (s/optional-key :test-while-idle?)                s/Bool
   (s/optional-key :time-between-eviction-runs-ms)   s/Int})

(s/defschema ^:private AuthHostPort
  {:host s/Str
   :port s/Int})

(s/defschema ^:private AuthURI
  {:uri s/Str})

(s/defschema ^:private AuthOptions
  {(s/optional-key :password)   s/Str
   (s/optional-key :timeout-ms) s/Int
   (s/optional-key :db)         s/Int})

(s/defschema ^:private ConnectionSpec
  (s/conditional
   #(u/contains-keys? % [:host :port])
   AuthHostPort

   #(u/contains-keys? % [:uri])
   AuthURI

   'valid-connection-spec?))

(s/defschema ConnectionOptions
  "Schema which validates a Redis ConnectionOptions map."
  {:pool (s/conditional
          #(= % :none) (s/eq :none)
          :else        PoolConfig)
   :spec (s/conditional
          #(= % {}) (s/eq {})
          :else     (s/maybe (merge ConnectionSpec AuthOptions)))})
