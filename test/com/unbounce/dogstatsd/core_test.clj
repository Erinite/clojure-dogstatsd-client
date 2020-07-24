(ns com.unbounce.dogstatsd.core-test
  (:require [clojure.spec.alpha :as s]
            [com.unbounce.dogstatsd.core :as sut]
            [clojure.test :refer [deftest testing is are] :as t]
            [clojure.spec.test.alpha :as stest])
  (:import [com.timgroup.statsd StatsDClient]))

(s/def ::sample-rate number?)
(s/def ::tags (s/coll-of string?))
(s/def ::opts (s/keys :opt-un [::tags ::sample-rate ::by]))

(s/fdef sut/str-array
        :args (s/cat :tags ::tags)
        :ret #(instance? java.lang.Object %))

(s/fdef sut/increment!
        :args (s/cat :client any?
                     :metric string?
                     :opts (s/? ::opts))
        :ret nil?)

(s/fdef sut/decrement!
        :args (s/cat :client any?
                     :metric string?
                     :opts (s/? ::opts))
        :ret nil?)


(s/fdef sut/gauge!
        :args (s/cat :client any?
                     :metric string?
                     :value number?
                     :opts (s/? ::opts))
        :ret nil?)


(s/fdef sut/histogram!
        :args (s/cat :client any?
                     :metric string?
                     :value number?
                     :opts (s/? ::opts))
        :ret nil?)

(deftest datadog-statsd-metrics
  (let [client (sut/init!)]
    (testing "datadog monitoring functions"
      (stest/instrument `sut/str-array)
      (stest/instrument `sut/increment!)
      (stest/instrument `sut/decrement!)
      (stest/instrument `sut/gauge!)
      (stest/instrument `sut/histogram!)

      (stest/check `sut/str-array)
      (stest/check `sut/increment!)
      (stest/check `sut/decrement!)
      (stest/check `sut/gauge!)
      (stest/check `sut/histogram!)

      (are [x y] (= x y)
        nil (sut/increment! client "asdf")
        nil (sut/increment! client "asdf" {:tags ["asdf"] :sample-rate 1})
        nil (sut/increment! client "asdf" {:by 2})

        nil (sut/decrement! client "asdf")
        nil (sut/decrement! client "asdf" {:tags ["asdf"] :sample-rate 1})
        nil (sut/decrement! client "asdf" {:by 2})

        nil (sut/gauge! client "asdf" 20)
        nil (sut/gauge! client "asdf" 20 {:tags ["asdf" "asdf"] :sample-rate 1})

        nil (sut/histogram! client "asdf" 20)
        nil (sut/histogram! client "asdf" 20 {:tags ["asdf" "asdf"] :sample-rate 1})))))

(deftest increment-decrement-test
  (let [calls (atom [])
        client (reify
                 StatsDClient
                 (^void count [self ^String aspect ^double delta ^"[Ljava.lang.String;" tags]
                   (swap! calls conj {:aspect aspect :delta delta :tags (into [] tags)}))
                 (^void count [self ^String aspect ^double delta ^double sample-rate ^"[Ljava.lang.String;" tags]
                   (swap! calls conj {:aspect aspect :delta delta :sample-rate sample-rate :tags (into [] tags)})))]
    (testing "simple increment"
      (reset! calls [])
      (sut/increment! client  "asdf")
      (is (= [{:aspect "asdf" :delta 1.0 :tags []}]
             @calls)))
    (testing "increment by value"
      (reset! calls [])
      (sut/increment! client "asdf" {:by 2})
      (is (= [{:aspect "asdf" :delta 2.0 :tags []}]
             @calls)))
    (testing "increment with sample rate"
      (reset! calls [])
      (sut/increment! client "asdf" {:sample-rate 3.14})
      (is (= [{:aspect "asdf" :delta 1.0 :sample-rate 3.14 :tags []}]
             @calls)))
    (testing "increment with tags"
      (reset! calls [])
      (sut/increment! client "asdf" {:sample-rate 3.14 :tags ["foo" "bar"]})
      (is (= [{:aspect "asdf" :delta 1.0 :sample-rate 3.14 :tags ["foo" "bar"]}]
             @calls)))
    (testing "simple decrement"
      (reset! calls [])
      (sut/decrement! client "asdf")
      (is (= [{:aspect "asdf" :delta -1.0 :tags []}]
             @calls)))
    (testing "decrement by value"
      (reset! calls [])
      (sut/decrement! client "asdf" {:by 2})
      (is (= [{:aspect "asdf" :delta -2.0 :tags []}]
             @calls)))
    (testing "decrement with sample rate"
      (reset! calls [])
      (sut/decrement! client "asdf" {:sample-rate 3.14})
      (is (= [{:aspect "asdf" :delta -1.0 :sample-rate 3.14 :tags []}]
             @calls)))
    (testing "decrement with tags"
      (reset! calls [])
      (sut/decrement! client "asdf" {:sample-rate 3.14 :tags ["foo" "bar"]})
      (is (= [{:aspect "asdf" :delta -1.0 :sample-rate 3.14 :tags ["foo" "bar"]}]
             @calls)))))

(deftest time!
  (let [result (atom nil)
        client (sut/init!)]
    (with-redefs [sut/histogram! (fn [c m v & opt] (reset! result m))]
      (testing "time! records duration of body"
        (sut/timed! client ["foo"]
          (Thread/sleep 100))
        (is (= "foo" @result)))

      (testing "time! records when exception is raised"
        (is (thrown? RuntimeException
                     (sut/timed! client ["bar"]
                       (Thread/sleep 100)
                       (throw (RuntimeException. "bar")))))
        (is (= "bar" @result))))))
