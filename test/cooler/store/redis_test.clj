(ns cooler.store.redis-test
  (:require [clojure.test :refer :all]
            [cooler.store.redis :refer :all]
            [cooler.store.core :refer :all]
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

(defn ^:private key-fn
  [k]
  (str "cooler:rate-limit-test:" k))

(deftest read-value-test
  (let [store (make-redis-store conn-opts key-fn)]
    (testing "present key"
      (let [present-raw-key "cooler:rate-limit-test:present-key"
            set-resp        (redis/wcar conn-opts
                                        (redis/set present-raw-key 123))
            start-ms        (System/currentTimeMillis)
            present-resp    (read-value store "present-key")]
        (is (= 123 (:value present-resp)))
        (is (<= start-ms (-> (:time-mus present-resp)
                             (/ 1000)
                             (long))))))

    (testing "missing key"
      (let [missing-resp (read-value store "missing-key")]
        (is (nil? (:value missing-resp)))))))

(deftest set-new-value-test
  (let [store (make-redis-store conn-opts key-fn)]
    (testing "new key"
      (let [set-resp (set-new-value! store "new-key" 123)]
        (is (true? set-resp))
        (is (= 123 (:value (read-value store "new-key"))))))

    (testing "existing key"
      (let [first-set-resp  (set-new-value! store "existing-key" 123)
            second-set-resp (set-new-value! store "existing-key" 456)
            read-resp       (read-value store "existing-key")]
        (is (true? first-set-resp))
        (is (false? second-set-resp))
        (is (= 123 (:value read-resp)))))

    (testing "ttl"
      (let [set-resp (set-new-value! store "ttl-key" 123 10)]
        (Thread/sleep 11)
        (let [read-resp (read-value store "ttl-key")]
          (is (true? set-resp))
          (is (nil? (:value read-resp))))))))

(deftest compare-and-update-test
  (let [store (make-redis-store conn-opts key-fn)]
    (testing "missing key"
      (let [missing-resp (compare-and-update! store "missing-key" 123 456)]
        (is (false? missing-resp))))

    (testing "successful CAU"
      (let [set-resp  (set-new-value! store "successful-cau-key" 123)
            cau-resp  (compare-and-update! store "successful-cau-key" 123 456)
            read-resp (read-value store "successful-cau-key")]
        (is (true? set-resp))
        (is (true? cau-resp))
        (is (= 456 (:value read-resp)))))

    (testing "unsuccessful CAU"
      (let [set-resp  (set-new-value! store "unsuccessful-cau-key" 123)
            cau-resp  (compare-and-update! store "unsuccessful-cau-key" 789 456)
            read-resp (read-value store "unsuccessful-cau-key")]
        (is (true? set-resp))
        (is (false? cau-resp))
        (is (= 123 (:value read-resp)))))))
