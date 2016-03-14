(ns cooler.rate-limiter-test
  (:require [clojure.test :refer :all]
            [cooler.core :refer :all]
            [cooler.rate-limiter.core :refer :all]
            [cooler.store.core :refer :all]
            [cooler.store.redis :refer :all]
            [schema.test :refer [validate-schemas]]
            [taoensso.carmine :as redis]))


(defn ^:private delete-keys-matching!
  [conn-opts pattern]
  (loop [[cursor elements] (redis/wcar conn-opts (redis/scan 0 :match pattern))]
    (redis/wcar conn-opts
                (doseq [element elements]
                  (redis/del element)))
    (when-not (= cursor "0")
      (recur (redis/wcar conn-opts (redis/scan 0 :match pattern))))))

(def ^:private conn-opts
  {:pool {} :spec {}})

(defn ^:private clean-redis!
  [f]
  (delete-keys-matching! conn-opts "cooler:rate-limit-test:*")
  (f)
  (delete-keys-matching! conn-opts "cooler:rate-limit-test:*"))

(use-fixtures :once (compose-fixtures validate-schemas clean-redis!))

(defrecord ^:private TestStore
  [store clock-ms fail-updates?]
  RateLimitStore
  (read-value [this k]
    (let [resp (read-value store k)]
      (assoc resp :time-mus clock-ms)))

  (set-new-value! [this k v]
    (if fail-updates?
      false
      (set-new-value! store k v)))

  (set-new-value! [this k v ttl-ms]
    (if fail-updates?
      false
      (set-new-value! store k v ttl-ms)))

  (compare-and-update! [this k old-val new-val]
    (if fail-updates?
      false
      (compare-and-update! store k old-val new-val)))

  (compare-and-update! [this k old-val new-val ttl-ms]
    (if fail-updates?
      false
      (compare-and-update! store k old-val new-val ttl-ms))))

(defn ^:private rate-limit-cases
  [start-ms]
  [[:rate-0  [start-ms             6 5 0       nil     true]]
   [:rate-1  [start-ms             1 4 1000000 nil     false]]
   [:rate-2  [start-ms             1 3 2000000 nil     false]]
   [:rate-3  [start-ms             1 2 3000000 nil     false]]
   [:rate-4  [start-ms             1 1 4000000 nil     false]]
   [:rate-5  [start-ms             1 0 5000000 nil     false]]
   [:rate-6  [start-ms             1 0 5000000 1000000 true]]
   [:rate-7  [(+ start-ms 3000000) 1 2 3000000 nil     false]]
   [:rate-8  [(+ start-ms 3100000) 1 1 3900000 nil     false]]
   [:rate-9  [(+ start-ms 4000000) 1 1 4000000 nil     false]]
   [:rate-10 [(+ start-ms 8000000) 1 4 1000000 nil     false]]
   [:rate-11 [(+ start-ms 9500000) 1 4 1000000 nil     false]]
   [:rate-12 [(+ start-ms 9500000) 0 4 1000000 nil     false]]
   [:rate-13 [(+ start-ms 9500000) 2 2 3000000 nil     false]]
   [:rate-14 [(+ start-ms 9500000) 5 2 3000000 3000000 true]]])

(deftest rate-limit-test
  (let [limit       5
        start-ms    0
        cases       (rate-limit-cases start-ms)
        key-fn      (fn [k] (str "cooler:rate-limit-test:" k))
        redis-store (make-redis-store conn-opts key-fn)]
    (doseq [[id [clock-ms quantity remaining reset-mus retry-mus limited?]] cases
            :let [test-store (->TestStore redis-store clock-ms false)
                  rl         (rate-limiter test-store 1 :second (- limit 1))
                  result     (rate-limit! rl "test-key" quantity)]]
      (is (= limited? (:limited? result))
          (str (name id) " should be limited"))

      (is (= limit (:limit result))
          (str (name id) " should have correct limit"))

      (is (= remaining (:remaining result))
          (str (name id) " should have correct remaining"))

      (is (= reset-mus (:reset-after-mus result))
          (str (name id) " should have correct reset-after-mus"))

      (is (= retry-mus (:retry-after-mus result))
          (str (name id) " should have correct retry-after-mus")))

    (let [test-store (->TestStore redis-store 0 true)
          rl         (rate-limiter test-store 1 :second 1)
          result     (rate-limit! rl "fail-test-key" 1)]
      (is (= nil result)))))
