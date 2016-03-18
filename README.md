# cooler

A Clojure rate limiting library which provides GCRA-based distributed limiting backed by Redis.

![The Cooler](http://i.imgur.com/80lJjxW.jpg)

*I want you to be nice... until it's time to not be nice.*


[![Clojars Project](https://img.shields.io/clojars/v/cooler.svg)](https://clojars.org/cooler)

## Usage

```clojure
(ns user
  (:require [cooler.core :as c]
            [cooler.rate-limiter.core :as crl]
            [cooler.store.redis :as csr))

;; Define a map of redis connection options. This follows the same format used
;; by the (awesome) clojure redis library carmine:
;; https://github.com/ptaoussanis/carmine#connections
(def conn-opts
  {:pool {}
   :spec {}})

;; Define a redis store and a rate-limiter backed by that store with a quota
;; of 30 requests per minute
(def redis-store (csr/make-redis-store conn-opts))
(def rate-limiter (c/rate-limiter redis-store 30 :minute))


;; The rate-limit method will return a RateLimitResult map which can be used
;; to determine the correct rate limiting action to take. 
(crl/rate-limit rate-limiter "test-limit" 1) ;; => {:limited?        false
                                             ;;     :limit           1
                                             ;;     :remaining       0
                                             ;;     :reset-after-mus 2000000
                                             ;;     :retry-after-mus nil}

;; Called again within two seconds
(crl/rate-limit rate-limiter "test-limit" 1) ;; => {:limited?        true
                                             ;;     :limit           1
                                             ;;     :remaining       0
                                             ;;     :reset-after-mus 1286882
                                             ;;     :retry-after-mus 1286882}
                                                    
;; Since the second request exceeded the rate-limiter's quota, the second result
;; indicates that:
;;
;;   * The request was rate limited
;;   * The quota request pool will reset back to the limit (in this case just one) after ~1.2 seconds
;;   * The next request which won't be limited is estimated to occur in ~1.2 seconds

```


## Project Status

***Important!*** - This library is still in early development. Please be careful!

That said, I will be using this library in a forthcoming commercial product, so I expect any issues found
will be fixed in short order. The current test coverage is equivalent to the tests performed by the golang
[throttled]() library which inspired cooler. As long as the tests pass (which they do at the time of this writing!),
this library should provide substantially similar behavior to that library.

If you find any issues, please do submit a PR! 


## TODO

* Improve documentation and examples
* Implement memory-backed store for times when distributed rate-limiting is unnecessary
* Clean GCRA implementation code


## Sponsors

Special thanks to [The Rainmaker Group](http://www.letitrain.com/) for sponsoring the development of this library.
If you're interested in professional Clojure development, check out the [careers](http://www.letitrain.com/careers) page for potential openings.

## License

Copyright © 2016 Dylan Paris

Distributed under the MIT License
