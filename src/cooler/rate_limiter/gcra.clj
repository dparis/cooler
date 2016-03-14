(ns cooler.rate-limiter.gcra
  "Implementation of GCRA rate limiting algorithm. For more details,
  see the following links:

  * https://brandur.org/rate-limiting
  * https://github.com/throttled/throttled
  * https://en.wikipedia.org/wiki/Generic_cell_rate_algorithm

  Credit to Brandur Leach and Andrew Metcalf for their work in implementing
  GCRA in the throttled library, upon which this implementation is based."
  (:require [cooler.rate-limiter.core :as rl]
            [cooler.schemas.rate-limit-result
             :refer [RateLimitResult]]
            [cooler.store.core :as st]
            [cooler.util :as u]
            [schema.core :as s]))


(def ^:private max-attempts 10)

(s/defn ^:private calc-next-tat
  [now tat increment]
  (if (> now tat)
    (+ now increment)
    (+ tat increment)))

(s/defn ^:private init-tat! :- s/Bool
  [store k now tat increment]
  (let [next-tat (+ now increment)
        ttl-mus  (- next-tat now)]
    (st/set-new-value! store k next-tat (u/mus->ms ttl-mus))))

(s/defn ^:private update-tat! :- s/Bool
  [store k now tat increment]
  (let [next-tat (calc-next-tat now tat increment)
        ttl-mus  (- next-tat now)]
    (st/compare-and-update! store k tat next-tat (u/mus->ms ttl-mus))))

(s/defn ^:private tat-update
  [limited? retry-after-mus ttl-mus]
  {:limited?        limited?
   :retry-after-mus retry-after-mus
   :ttl-mus         ttl-mus})

(defn ^:private perform-rate-limit!
  ([store k increment dvt]
   (perform-rate-limit! store k increment dvt 0))
  ([store k increment dvt attempt-count]
   (when (< attempt-count max-attempts)
     (let [rv-resp  (st/read-value store k)
           now      (:time-mus rv-resp)
           tat-val  (:value rv-resp)
           tat      (or tat-val now)
           next-tat (calc-next-tat now tat increment)
           allow-at (- next-tat dvt)
           req-diff (- now allow-at)
           ttl-mus  (if (< req-diff 0) (- tat now) (- next-tat now))]
       ;; If tat-val is nil, a tat has not been set for k in store.
       (if-not tat-val
         ;; When tat-val is nil, attempt to init the tat for k.
         ;; If init-tat! fails, attempt perform-rate-limit! again.
         (if-not (init-tat! store k now tat increment)
           (perform-rate-limit! store k increment dvt (inc attempt-count))
           (tat-update false nil ttl-mus))

         ;; When tat-val is not nil, attempt to update tat.
         (if (< req-diff 0)
           (tat-update true (when (<= increment dvt) (- allow-at now)) ttl-mus)
           (if-not (update-tat! store k now tat increment)
             (perform-rate-limit! store k increment dvt (inc attempt-count))
             (tat-update false nil ttl-mus))))))))

(defn ^:private calc-remaining
  [dvt ttl-mus emission-interval]
  (let [next-mus (- dvt ttl-mus)]
    (when (> next-mus (- emission-interval))
      (int (/ next-mus emission-interval)))))

(defn ^:private rate-limit-result
  [limited? limit remaining reset-after-mus retry-after-mus]
  {:limited?        limited?
   :limit           limit
   :remaining       remaining
   :reset-after-mus reset-after-mus
   :retry-after-mus retry-after-mus})

(s/defn ^:private gcra-rate-limit! :- (s/maybe RateLimitResult)
  [store             :- (s/protocol st/RateLimitStore)
   k                 :- s/Str
   quantity          :- s/Int
   limit             :- s/Int
   emission-interval :- s/Int
   dvt               :- s/Int]
  (if (> quantity limit)
    (let [prl-resp  (perform-rate-limit! store k 0 dvt)
          ttl-mus   (:ttl-mus prl-resp)
          retry-mus (:retry-after-mus prl-resp)
          remaining (calc-remaining dvt ttl-mus emission-interval)]
      (rate-limit-result true limit remaining ttl-mus retry-mus))
    (let [increment (* quantity emission-interval)
          prl-resp  (perform-rate-limit! store k increment dvt)]
      (when prl-resp
        (let [limited?  (:limited? prl-resp)
              ttl-mus   (:ttl-mus prl-resp)
              retry-mus (:retry-after-mus prl-resp)
              remaining (calc-remaining dvt ttl-mus emission-interval)]
          (rate-limit-result limited? limit remaining ttl-mus retry-mus))))))

(s/defrecord ^:private GCRARateLimiter
  [store             :- (s/protocol st/RateLimitStore)
   limit             :- s/Int
   emission-interval :- s/Int
   dvt               :- s/Int]
  rl/RateLimiter
  (rate-limit! [this k quantity]
    (gcra-rate-limit! store k quantity limit emission-interval dvt)))

(s/defn make-gcra-rate-limiter
  "Returns a GCRARateLimiter which implements the RateLimiter protocol."
  [store amount unit burst]
  (let [period (u/period-mus amount unit)
        limit  (+ burst 1)
        dvt    (* period limit)]
    (->GCRARateLimiter store limit period dvt)))
