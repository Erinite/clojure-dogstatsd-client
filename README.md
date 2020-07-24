# clojure-statsd-client

![Clojars Project](https://img.shields.io/clojars/v/erinite/clojure-dogstatsd-client.svg)

---

**Fork of [unbounce/clojure-dogstatsd-client](https://github.com/unbounce/clojure-dogstatsd-client) with some cosmetic changes:**

 * `erinite/clojure-dogstatsd-client` appends a `!` to all of the functions, eg `increment!` vs `increment` in `unbounce/clojure-dogstatsd-client`
 * `erinite/clojure-dogstatsd-client` does not keep global state, taking the client as first argument  eg `(increment! client "counter")` vs `(increment "counter")` in `unbounce/clojure-dogstatsd-client`
 * `erinite/clojure-dogstatsd-client` uses `init!` to create and return a client object, `unbounce/clojure-dogstatsd-client` uses `setup!` to setup a global client object
 * `erinite/clojure-dogstatsd-client` passes config as maps to `init!`: `(init! {:host "foo"})`, `unbounce/clojure-dogstatsd-client` uses pairs of arguments: `(setup! :host "foo")`
 * `erinite/clojure-dogstatsd-client` uses `halt!` to stop a passed in client object, `unbounce/clojure-dogstatsd-client` uses `shutdown!` to stop a global client object
 * `erinite/clojure-dogstatsd-client` uses `(timed! client ...)` to time some code, `unbounce/clojure-dogstatsd-client` uses `(time! ...)`
 * To create a no-op client, call `init!` without arguments: `(init!)`
 * The only function whose name was left unchanged is `wrap-http-metrics` (which now takes a client as first argument), otherwise the names don't clash and this fork could easily provide compatible wrapper functions

Special Thanks to Unbounce Marketing Solutions Inc. for creating clojure-dogstatsd-client!

---

A thin veneer over the officia Java dogstatsd
[client](https://github.com/DataDog/java-dogstatsd-client). This library favours
pragmatism where possible.

Instrumenting your code should be easy, you shouldn't be forced to thread a
statsd client in your code. This library keeps a global client around so your
application doesn't need to know about it.

## Usage

```
[erinite/clojure-dogstatsd-client "0.6.3"]
```

Somewhere in your code, you should setup the client:

``` clojure
(require '[com.unbounce.dogstatsd.core :as statsd])

;; Do this once in your code
;; Or statd calls will default to use NoOpStatsDClient to avoid nullpointer exception
;; You can also configure the host/port by setting the environment variables: DD_AGENT_HOST and DD_DOGSTATSD_PORT
(let [client (statsd/init! {:host "127.0.0.1" :port 8125 :prefix "my.app"})]

  ;; Increment or decrement a counter
  (statsd/increment! client "counter")           ; increment by 1
  (statsd/increment! client "counter" {:by 2.5}) ; increment by 2.5
  (statsd/decrement! client "another.counter")   ; decrement by 1

  ;; Records a value at given time
  (statsd/gauge! client "a.gauge" 10)

  ;; Record a histogram value (i.e for measuring percentiles)
  (statsd/histogram! client "a.histogram" 10)

  ;; Time how long body takes and records it to the metric
  (statsd/timed! client ["a.timed.body" {}]
    (Thread/sleep 100)
    (Thread/sleep 100))

  ;; Time how long it takes with a tag/sample-rate
  (statsd/timed! client ["my.metric.with.tags" {:tags #{"foo"} :sample-rate 0.3}}]
    (Thread/sleep 1000))

  ;; Shutdown client to ensure all messages are emitted to statsd and resources are cleaned up
  (statsd/halt! client))
```

### Ring Middleware

This library also has comes with a ring middleware to capture HTTP requests.
See `com.unbounce.dogstatsd.ring` for more information.

The middleware provides these metrics:

- http.1xx  counter of 1xx responses
- http.2xx  counter of 2xx responses
- http.3xx  counter of 3xx responses
- http.4xx  counter of 4xx responses
- http.5xx  counter of 5xx responses
- http.count     counter for total requests
- http.exception counter for exceptions raised
- http.duration  histogram of request duration

Usage:

```
(require '[com.unbounce.dogstatsd.ring :as dogstatsd.ring])
(require '[com.unbounce.dogstatsd.core :refer [init!]])

(let [client (init! {:host "127.0.0.1" :port 8125})]
  ;; by default instrument all requests
  (def handler (->> (constantly {:status 200})
                  (dogstatsd.ring/wrap-http-metrics client)))

  ;; when sample-rate is set, only 20% of requests will be instrumented
  (def handler (dogstatsd.ring/wrap-http-metrics client (constantly {:status 200}) {:sample-rate 0.2})))
```



## License

Copyright © 2018 Unbounce Marketing Solutions Inc.

Distributed under the MIT License.
