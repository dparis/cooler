(ns cooler.store.core
  "Core definition of RateLimitStore protocol and related functionality.")

(defprotocol RateLimitStore
  (read-value [this k])
  (set-new-value! [this k v] [this k v ttl-ms])
  (compare-and-update! [this k old-val new-val] [this k old-val new-val ttl-ms]))
