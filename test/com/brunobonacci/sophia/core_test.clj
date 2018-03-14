(ns com.brunobonacci.sophia.core-test
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [com.brunobonacci.sophia :as db]
            [clojure.string :as str]
            [clojure.java.io :as io]))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| U T I L I T I E S |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn uuid []
  (str (java.util.UUID/randomUUID)))



(defn rand-db-name [name]
  (str "/tmp/sophia-" name "-" (uuid)))



(defn rand-db [name]
  (db/sophia {:sophia.path (rand-db-name name) :db name}))



(defn rm-fr
  [f & {:keys [force] :or {force true}}]
  (let [^java.io.File f (io/file f)]
    (if (.isDirectory f)
      (run! #(rm-fr % :force force) (.listFiles f)))
    (io/delete-file f force)))



(defmacro with-test-database
  "bindings => [name init ...]
  Evaluates body in a try expression with names bound to the values
  of the inits, and a finally clause that calls (.close name) on each
  name in reverse order."
  [bindings & body]
  (assert (vector? bindings) "a vector for its binding")
  (assert (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-test-database ~(subvec bindings 2) ~@body)
                                (finally
                                  (when-let [path# (-> ~(bindings 0) :config :sophia.path)]
                                    (rm-fr path#)))))
    :else (throw (IllegalArgumentException.
                  "with-test-database only allows Symbols in bindings"))))



(def sequecen-data
  (->>
   (for [x (range 3)
       y (range 10)]
     (format "%d%03d" x y))
   (map (juxt identity identity))))



(defn load-seqence-data [sophia db]
  (doseq [[k v] sequecen-data]
    (db/set-value! sophia db k v)))



(defmacro test-range-query
  [sophia db & opts]
  `(with-open [cursor# (db/cursor ~sophia)]
     (doall (db/range-query cursor# ~db ~@opts))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ----==| T E S T . C H E C K |==----                     ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def num-tests
  (or
   (println "TC_NUM_TESTS=" (or (System/getenv "TC_NUM_TESTS") 100))
   (Integer/getInteger "test-check.num-tests")
   (some-> (System/getenv "TC_NUM_TESTS") Integer/parseInt)
   100))


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



(with-test-database [sophia (rand-db "test")]
  (let [test
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
          (:result test) => true)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ----==| R A N G E - Q U E R Y |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(facts "range-query - on  empty db"

       (with-test-database [sophia (rand-db "test")]

         (test-range-query sophia "test")) => []
         )



(facts "range-query - cursor closed"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (count
          (with-open [cursor (db/cursor sophia)]
            (db/range-query cursor "test"))))
       => (throws #"Cursor already closed.")
       )



(facts "range-query - full index scan"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test"))  => sequecen-data
       )




(facts "range-query - full index scan - descending order"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test"  :order :desc))
       => (reverse sequecen-data)
         )



(facts "range-query - prefix"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1" :search-type :prefix))
       => (->> sequecen-data (filter #(str/starts-with? (first %) "1")))
         )



(facts "range-query - non matching prefix"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "ABC" :search-type :prefix))
       => []
         )



(facts "range-query - prefix - descending order not working with prefix"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1" :search-type :prefix
                         :order :desc))
       => (->> sequecen-data (filter #(str/starts-with? (first %) "1")))
         )



(facts "range-query - index scan prefix"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1"
                         :search-type :index-scan-inclusive))
       => (->> sequecen-data (drop 10))
         )



(facts "range-query - index scan inclusive"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1000"
                         :search-type :index-scan-inclusive))
       => (->> sequecen-data (drop 10))
         )



(facts "range-query - index scan exclusive"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1000"
                         :search-type :index-scan-exclusive))
       => (->> sequecen-data (drop 11))
         )



(facts "range-query - index scan not matching"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "ABC"
                         :search-type :index-scan-inclusive))
       => []
         )



(facts "range-query - index scan prefix - descending order"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1"
                         :search-type :index-scan-inclusive
                         :order :desc))
       => (->> sequecen-data (take 10) reverse)
         )



(facts "range-query - index scan inclusive - descending order"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1000"
                         :search-type :index-scan-inclusive
                         :order :desc))
       => (->> sequecen-data (take 11) reverse)
         )



(facts "range-query - index scan exclusive - descending order"

       (with-test-database [sophia (rand-db "test")
             _ (load-seqence-data sophia "test")]

         (test-range-query sophia "test" :key "1000"
                         :search-type :index-scan-exclusive
                         :order :desc))
       => (->> sequecen-data (take 10) reverse)
         )
