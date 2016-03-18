(ns cooler.store.redis
  "Implementation of RateLimitStore which uses Redis as a backing store.
  Requires at least redis 2.6, as it makes use of PEXPIRE to provide
  millisecond resolution for TTL values."
  (:require [clojure.java.io :as io]
            [cooler.schemas.redis.connection-options
             :refer [ConnectionOptions]]
            [cooler.store.core :as cs]
            [cuerdas.core :as str]
            [schema.core :as s]
            [taoensso.carmine :as redis]))


(s/defschema ^:private RedisReadValueResponse
  {:time-mus s/Int
   :value    (s/maybe s/Int)})

(s/defn ^:private parse-redis-time :- s/Int
  [time-parts :- (s/pair s/Str "epoch-ms" s/Str "cur-mus")]
  (let [pad-opts {:length  6
                  :padding "0"}
        mus-part (str/pad (last time-parts) pad-opts)]
    (-> [(first time-parts) mus-part]
        (str/join)
        (str/parse-long))))

(s/defn ^:private redis-read-value :- RedisReadValueResponse
  [conn-opts :- ConnectionOptions
   k         :- s/Str]
  (let [[time-parts v] (redis/wcar conn-opts
                                   (redis/time)
                                   (redis/get k))
        value-long     (when v (Long/parseLong v))
        read-time-mus  (parse-redis-time time-parts)]
    {:time-mus read-time-mus
     :value    value-long}))

(s/defn ^:private redis-set-new-value! :- s/Bool
  ([conn-opts :- ConnectionOptions
    k         :- s/Str
    v         :- s/Int]
   (redis-set-new-value! conn-opts k v 0))
  ([conn-opts :- ConnectionOptions
    k         :- s/Str
    v         :- s/Int
    ttl-ms    :- s/Int]
   (let [resp (redis/wcar conn-opts
                          (redis/setnx k v))
         set? (= 1 resp)]
     (when (and set? (>= ttl-ms 1))
       (redis/wcar conn-opts
                   (redis/pexpire k ttl-ms)))
     set?)))

(def ^:private redis-cau-script
  (slurp (io/resource "redis_cau_script.lua")))

(def ^:private redis-cau-key-error
  "key does not exist")

(s/defn ^:private redis-compare-and-update! :- s/Bool
  ([conn-opts :- ConnectionOptions
    k         :- s/Str
    old-val   :- s/Int
    new-val   :- s/Int]
   (redis-compare-and-update! conn-opts k old-val new-val 0))
  ([conn-opts :- ConnectionOptions
    k         :- s/Str
    old-val   :- s/Int
    new-val   :- s/Int
    ttl-ms    :- s/Int]
   (let [cau-keys {:cau-key k}
         cau-args {:cau-old-val old-val
                   :cau-new-val new-val
                   :cau-ttl-ms  ttl-ms}]
     (try
       (let [resp (redis/wcar conn-opts
                              (redis/lua redis-cau-script
                                         cau-keys
                                         cau-args))]
         (= 1 resp))
       (catch clojure.lang.ExceptionInfo ex
         (if (= redis-cau-key-error (.getMessage ex))
           false
           (throw ex)))))))

(s/defrecord ^:private RedisRateLimitStore
  [conn-opts :- ConnectionOptions
   key-fn    :- (s/=> s/Str s/Str)]
  cs/RateLimitStore
  (read-value [this k]
    (redis-read-value conn-opts (key-fn k)))
  (set-new-value! [this k v]
    (redis-set-new-value! conn-opts (key-fn k) v))
  (set-new-value! [this k v ttl-ms]
    (redis-set-new-value! conn-opts (key-fn k) v ttl-ms))
  (compare-and-update! [this k old-val new-val]
    (redis-compare-and-update! conn-opts (key-fn k) old-val new-val))
  (compare-and-update! [this k old-val new-val ttl-ms]
    (redis-compare-and-update! conn-opts (key-fn k) old-val new-val)))

(s/defn make-redis-store :- RedisRateLimitStore
  "Returns a RedisRateLimitStore which implements the
  RateLimitStore protocol."
  ([conn-opts :- ConnectionOptions]
   (let [key-fn (fn [k] (str "cooler:rate-limit:" k))]
     (make-redis-store conn-opts key-fn)))
  ([conn-opts :- ConnectionOptions
    key-fn    :- (s/=> s/Str s/Str)]
   (->RedisRateLimitStore conn-opts key-fn)))
