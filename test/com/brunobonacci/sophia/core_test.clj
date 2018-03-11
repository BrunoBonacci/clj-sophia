(ns com.brunobonacci.sophia.core-test
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [com.brunobonacci.sophia :as db]
            [clojure.string :as str]))



(def num-tests
  (or
   (println "TC_NUM_TESTS=" (or (System/getenv "TC_NUM_TESTS") 100))
   (Integer/getInteger "test-check.num-tests")
   (some-> (System/getenv "TC_NUM_TESTS") Integer/parseInt)
   100))



(defn uuid []
  (str (java.util.UUID/randomUUID)))


(defn rand-db-name [name]
  (str "/tmp/sophia-" name "-" (uuid)))


(defn rand-db [name]
  (db/sophia {:sophia.path (rand-db-name name) :db name}))

(def sequecen-data
  (->>
   (for [x (range 3)
       y (range 10)]
     (format "%d%03d" x y))
   (map (juxt identity identity))))


(defn load-seqence-data [sophia db]
  (doseq [[k v] sequecen-data]
    (db/set-value! sophia db k v)))

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



(let [sophia (rand-db "test")
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
  (fact "set then get symmetry"
   (:result test) => true))



(facts "range-query - full index scan"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test") => sequecen-data
        ))



(facts "range-query - full index scan - descending order"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test"  :order :desc)
         => (reverse sequecen-data)
         ))



(facts "range-query - prefix"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1" :search-type :prefix)
         => (->> sequecen-data (filter #(str/starts-with? (first %) "1")))
         ))


(facts "range-query - prefix - descending order not working with prefix"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1" :search-type :prefix
                         :order :desc)
         => (->> sequecen-data (filter #(str/starts-with? (first %) "1")))
         ))



(facts "range-query - index scan prefix"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1"
                         :search-type :index-scan-inclusive)
         => (->> sequecen-data (drop 10))
         ))



(facts "range-query - index scan inclusive"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1000"
                         :search-type :index-scan-inclusive)
         => (->> sequecen-data (drop 10))
         ))



(facts "range-query - index scan exclusive"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1000"
                         :search-type :index-scan-exclusive)
         => (->> sequecen-data (drop 11))
         ))



(facts "range-query - index scan prefix - descending order"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1"
                         :search-type :index-scan-inclusive
                         :order :desc)
         => (->> sequecen-data (take 10) reverse)
         ))



(facts "range-query - index scan inclusive - descending order"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1000"
                         :search-type :index-scan-inclusive
                         :order :desc)
         => (->> sequecen-data (take 11) reverse)
         ))



(facts "range-query - index scan exclusive - descending order"

       (let [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (db/range-query sophia "test" :key "1000"
                         :search-type :index-scan-exclusive
                         :order :desc)
         => (->> sequecen-data (take 10) reverse)
         ))
