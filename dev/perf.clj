(ns perf
  (:require [com.brunobonacci.sophia :as sph]
            [criterium.core :refer [bench quick-bench]]
            [clojure.string :as str]))



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
  ;; low-severe  3 (5.0000 %)
  ;; Variance from outliers : 43.4703 % Variance is moderately inflated by outliers




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ---==| G E T - V A L U E |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;; round up keys up to 5M
  (let [nkeys (with-open [^java.io.Closeable cur (sph/cursor env)]
                (count (sph/range-query cur "perf")))]
    (when (> nkeys 5000000)
      (throw (ex-info "Too many keys in db." {:nkeys nkeys})))
    ;; add more keys up to 5M
    (dotimes [_ (- 5000000 nkeys)]
      (sph/set-value!  env "perf" (uuid) (uuid))))

  ;; get 100 random keys
  (def ids
    (->>
     (with-open [cur (sph/cursor env)]
       (doall
        (take 100
              (partition 50000
                         (sph/range-query cur "perf")))))
     (mapv ffirst)))

  (count ids)

  (bench (rand-nth ids)) ;; 147.352034 ns

  (bench
   (sph/get-value env "perf" (rand-nth ids)))


  ;; Evaluation count : 3338580 in 60 samples of 55643 calls.
  ;; Execution time mean : 18.583898 µs
  ;; Execution time std-deviation : 633.802589 ns
  ;; Execution time lower quantile : 17.670723 µs ( 2.5%)
  ;; Execution time upper quantile : 19.701369 µs (97.5%)
  ;; Overhead used : 1.811818 ns
  ;;
  ;; Found 1 outliers in 60 samples (1.6667 %)
  ;; low-severe  1 (1.6667 %)
  ;; Variance from outliers : 20.6156 % Variance is moderately inflated by outliers



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| R A N G E - Q U E R Y |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  (defn rand-prefix []
    (-> (rand-nth ids)
        (subs 0 8)))

  (bench (rand-prefix)) ;; 152.616500 ns

  ;; seek time
  (bench
   (with-open [^java.io.Closeable cur (sph/cursor env)]
     (first (sph/range-query cur "perf" :key (rand-prefix)))))

  ;; Evaluation count : 1398900 in 60 samples of 23315 calls.
  ;; Execution time mean : 45.553131 µs
  ;; Execution time std-deviation : 3.049912 µs
  ;; Execution time lower quantile : 40.749923 µs ( 2.5%)
  ;; Execution time upper quantile : 49.009489 µs (97.5%)
  ;; Overhead used : 1.811818 ns
  ;;
  ;; Found 1 outliers in 60 samples (1.6667 %)
  ;; low-severe  1 (1.6667 %)
  ;; Variance from outliers : 50.1159 % Variance is severely inflated by outliers


  ;; scan time
  (println
   "count:"
   (time
    (with-open [^java.io.Closeable cur (sph/cursor env)]
      (count (sph/range-query cur "perf")))))

  ;; "Elapsed time: 40457.261344 msecs"
  ;; count: 5000000
  ;; (/ (* 1000 86066.752595) 5000000 ) => 17.213350519 µs


  )
