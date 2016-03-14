(ns cooler.rate-limiter.core
  "Core definition of RateLimiter protocol and related functionality.")

(defprotocol RateLimiter
  (rate-limit! [this k quantity]
    "Requests a rate limit for a key k and quantity within a given RateLimiter.
    Returns a RateLimitResult map which can be used to determine the appropriate
    reaction to being rate-limited."))
