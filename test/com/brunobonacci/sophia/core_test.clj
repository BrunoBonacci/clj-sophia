(ns com.brunobonacci.sophia.core-test
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.brunobonacci.sophia :as db]))



(def num-tests
  (or
   (println "TC_NUM_TESTS=" (or (System/getenv "TC_NUM_TESTS") 100))
   (Integer/getInteger "test-check.num-tests")
   (some-> (System/getenv "TC_NUM_TESTS") Integer/parseInt)
   100))



(defn uuid []
  (str (java.util.UUID/randomUUID)))



;; workaround for https://dev.clojure.org/jira/browse/CLJ-2334
(def any-non-NaN
  "A recursive generator that will generate many different, often nested, values"
  (gen/recursive-gen gen/container-type
                     (gen/one-of [(gen/double* {:NaN? false})
                                  gen/int gen/large-integer
                                  gen/char gen/string gen/ratio
                                  gen/boolean gen/keyword gen/keyword-ns gen/symbol
                                  gen/symbol-ns gen/uuid])))


(def any-value-gen
  (gen/frequency [[99 any-non-NaN] [1 (gen/return nil)]]))



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
