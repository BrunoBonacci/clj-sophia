(ns com.brunobonacci.sophia.core-test
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.brunobonacci.sophia :as db]))



(def num-tests
  (or
   (Integer/getInteger "test-check.num-tests")
   (some-> (System/getenv "TC_NUM_TESTS") Integer/parseInt)
   100))



(defn uuid []
  (str (java.util.UUID/randomUUID)))



(def any-value-gen
  (gen/frequency [[99 gen/any] [1 (gen/return nil)]]))



(deftest set-then-get-symmetry
  (let [sophia (db/sophia {:sophia.path (str "/tmp/sophia-test-" (uuid)) :db "test"})
        test
        (tc/quick-check
         num-tests
         (prop/for-all
          [key   (gen/not-empty gen/string-ascii)
           value any-value-gen]

          ;;(println (format "Testing SET/GET '%s' -> '%s'" key value))
          ;; set the key
          (db/set-value! sophia "test" key value)
          ;; get the value and check the result
          (= value (db/get-value sophia "test" key))))]
    (pprint test)
    (is (:result test))))
