(ns perf
  (:require [com.brunobonacci.sophia]
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

  ;; Evaluation count : 1495620 in 60 samples of 24927 calls.
  ;; Execution time mean : 40.612923 µs (- 2x 1.827076 µs)
  ;; Execution time std-deviation : 1.087180 µs
  ;; Execution time lower quantile : 39.203336 µs ( 2.5%)
  ;; Execution time upper quantile : 42.085349 µs (97.5%)
  ;; Overhead used : 1.816742 ns
  ;;
  ;; Found 1 outliers in 60 samples (1.6667 %)
  ;; low-severe	 1 (1.6667 %)
  ;; Variance from outliers : 14.1823 % Variance is moderately inflated by outliers




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
              (partition 20000
                         (sph/range-query cur "perf")))))
     (mapv ffirst)))


  (bench
   (sph/get-value env "perf" (rand-nth ids)))


  ;; Evaluation count : 2644860 in 60 samples of 44081 calls.
  ;; Execution time mean : 22.833318 µs
  ;; Execution time std-deviation : 886.208817 ns
  ;; Execution time lower quantile : 22.199158 µs ( 2.5%)
  ;; Execution time upper quantile : 25.428661 µs (97.5%)
  ;; Overhead used : 1.816742 ns
  ;;
  ;; Found 3 outliers in 60 samples (5.0000 %)
  ;; low-severe	 3 (5.0000 %)
  ;; Variance from outliers : 25.4373 % Variance is moderately inflated by outliers



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


  (bench
   (with-open [cur (sph/cursor env)]
     (first (sph/range-query cur "perf" :key (rand-prefix)))))

  ;;  Evaluation count : 1080120 in 60 samples of 18002 calls.
  ;;  Execution time mean : 57.442475 µs (- 2.680685 µs)
  ;;  Execution time std-deviation : 1.100158 µs
  ;;  Execution time lower quantile : 54.944727 µs ( 2.5%)
  ;;  Execution time upper quantile : 59.610904 µs (97.5%)
  ;;  Overhead used : 1.816742 ns
  ;;
  ;;  Found 1 outliers in 60 samples (1.6667 %)
  ;;  low-severe	 1 (1.6667 %)
  ;;  Variance from outliers : 7.8403 % Variance is slightly inflated by outliers


  (time
   (with-open [cur (sph/cursor env)]
     (count (sph/range-query cur "perf"))))



  )
