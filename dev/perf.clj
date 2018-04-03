(ns perf
  (:require [com.brunobonacci.sophia :as sph]
            [criterium.core :refer [bench quick-bench]]))



(def uuid #'com.brunobonacci.sophia/uuid)


(comment

  ;; Clojure 1.9.0, Java 1.8.0_45 on Intel Core i7 2.9 GHz / 16 GB 2133 MHz LPDDR3
  ;; clj-sophia v0.4.2

  (def env (sph/sophia {:sophia.path "/tmp/sophia-perf"
                        :dbs ["perf"]}))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ---==| S E T - V A L U E ! |==----                     ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (bench (uuid)) ;; 1.827076 µs

  (bench
   (sph/set-value!  env "perf" (uuid) (uuid)))

  ;; Evaluation count : 1993380 in 60 samples of 33223 calls.
  ;; Execution time mean : 31.940635 µs (- 2x 1.827076 µs) => *~28.3 µs*
  ;; Execution time std-deviation : 1.875344 µs
  ;; Execution time lower quantile : 29.803801 µs ( 2.5%)
  ;; Execution time upper quantile : 36.487102 µs (97.5%)
  ;; Overhead used : 2.086444 ns
  ;;
  ;; Found 3 outliers in 60 samples (5.0000 %)
  ;; low-severe	 3 (5.0000 %)
  ;; Variance from outliers : 43.4703 % Variance is moderately inflated by outliers




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ---==| G E T - V A L U E |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  ;; get 100 random keys
  (def ids
    (->>
     (with-open [cur (sph/cursor env)]
       (doall
        (take 100
              (partition 15000
                         (sph/range-query cur "perf")))))
     (mapv ffirst)))

  (count ids)

  (bench (rand-nth ids)) ;; 147.352034 ns

  (bench
   (sph/get-value env "perf" (rand-nth ids)))


  ;; Evaluation count : 3508320 in 60 samples of 58472 calls.
  ;; Execution time mean : 17.593094 µs
  ;; Execution time std-deviation : 294.678938 ns
  ;; Execution time lower quantile : 17.190567 µs ( 2.5%)
  ;; Execution time upper quantile : 18.315122 µs (97.5%)
  ;; Overhead used : 2.086444 ns
  ;;
  ;; Found 3 outliers in 60 samples (5.0000 %)
  ;; low-severe	 3 (5.0000 %)
  ;; Variance from outliers : 6.2541 % Variance is slightly inflated by outliers



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| R A N G E - Q U E R Y |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  (defn rand-prefix []
    (->> (repeatedly #(rand-nth '("0" "1" "2" "3" "4" "5" "6" "7" "8" "a" "b" "c" "d" "e" "f")))
         (take 8)
         (apply str)))

  (bench (rand-prefix)) ;; 3.415691 µs

  ;; seek time
  (bench
   (with-open [^java.io.Closeable cur (sph/cursor env)]
     (first (sph/range-query cur "perf" :key (rand-prefix)))))

  ;; Evaluation count : 1240320 in 60 samples of 20672 calls.
  ;; Execution time mean : 51.623567 µs (- 3.415691 µs) => ~*48.2 µs*
  ;; Execution time std-deviation : 3.866012 µs
  ;; Execution time lower quantile : 45.745753 µs ( 2.5%)
  ;; Execution time upper quantile : 57.400526 µs (97.5%)
  ;; Overhead used : 2.086444 ns


  ;; scan time
  (println
   "count:"
   (time
    (with-open [^java.io.Closeablecur (sph/cursor env)]
      (count (sph/range-query cur "perf")))))

  ;; "Elapsed time: 41259.694217 msecs"
  ;; count: 2413142
  ;; (/ (* 1000 41259.694217) 2413142 ) => 17.097913930054677 µs


  )
