(ns perf
  (:require [com.brunobonacci.sophia :as sph]
            [criterium.core :refer [bench quick-bench]]))



(def uuid #'com.brunobonacci.sophia/uuid)


(comment

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

  ;; Evaluation count : 2069280 in 60 samples of 34488 calls.
  ;; Execution time mean : 29.061050 µs ( - 2x 1.827076 µs) ==> *25.5 µs*
  ;; Execution time std-deviation : 882.068886 ns
  ;; Execution time lower quantile : 27.913785 µs ( 2.5%)
  ;; Execution time upper quantile : 31.006612 µs (97.5%)
  ;; Overhead used : 2.107959 ns
  ;;
  ;; Found 2 outliers in 60 samples (3.3333 %)
  ;; low-severe  1 (1.6667 %)
  ;; low-mild    1 (1.6667 %)
  ;; Variance from outliers : 17.3832 % Variance is moderately inflated by outliers




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


  ;; Evaluation count : 4162200 in 60 samples of 69370 calls.
  ;; Execution time mean : 15.012568 µs
  ;; Execution time std-deviation : 782.143385 ns
  ;; Execution time lower quantile : 14.410598 µs ( 2.5%)
  ;; Execution time upper quantile : 15.629390 µs (97.5%)
  ;; Overhead used : 2.107959 ns
  ;;
  ;; Found 2 outliers in 60 samples (3.3333 %)
  ;; low-severe  1 (1.6667 %)
  ;; low-mild    1 (1.6667 %)
  ;; Variance from outliers : 38.4649 % Variance is moderately inflated by outliers



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| R A N G E - Q U E R Y |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  (defn rand-prefix []
    (->> (repeatedly #(rand-nth '("0" "1" "2" "3" "4" "5" "6" "7" "8" "a" "b" "c" "d" "e" "f")))
         (take 8)
         (apply str)))

  (bench (rand-prefix)) ;; 2.680685 µs

  ;; seek time
  (bench
   (with-open [cur (sph/cursor env)]
     (first (sph/range-query cur "perf" :key (rand-prefix)))))

  ;; Evaluation count : 1282620 in 60 samples of 21377 calls.
  ;; Execution time mean : 47.575864 µs (- 2.680685 µs) => 45 µs
  ;; Execution time std-deviation : 816.744321 ns
  ;; Execution time lower quantile : 46.094277 µs ( 2.5%)
  ;; Execution time upper quantile : 49.356568 µs (97.5%)
  ;; Overhead used : 2.107959 ns
  ;;
  ;; Found 2 outliers in 60 samples (3.3333 %)
  ;; low-severe  2 (3.3333 %)
  ;; Variance from outliers : 6.2739 % Variance is slightly inflated by outliers


  ;; scan time
  (time
   (with-open [cur (sph/cursor env)]
     (count (sph/range-query cur "perf"))))

  ;; "Elapsed time: 27785.393502 msecs"
  ;; count: 2502245
  ;; (/ (* 1000 27785.393502) 2502245 ) => 11.104185841913962 µs


  )
