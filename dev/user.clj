(ns user
  (:require [com.brunobonacci.sophia :as sph]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                ----==| G E T / S E T / D E L E T E |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def env (sph/sophia {:sophia.path "/tmp/sophia-test"
                      :db "accounts"}))


;; set a simple value
(sph/set-value!  env "accounts" "user1" "John")
;;=> :ok

;; get the value back
(sph/get-value   env "accounts" "user1")
;;=> "John"

;; delete a key
(sph/delete-key! env "accounts" "user1")
;;=> :ok

;; now the key isn't present
(sph/get-value   env "accounts" "user1")
;;=> nil

;; set a complex value
(sph/set-value! env "accounts" "user1"
                {:firstname "John" :lastname "Doe" :age 34 :balance 100.0})
;;=> :ok

;; get it back
(sph/get-value   env "accounts" "user1")
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}

(sph/set-value! env "accounts" "user2"
                {:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0})
;;=> :ok

(sph/set-value! env "accounts" "admin1"
                {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]})
;;=> :ok



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ----==| R A N G E - Q U E R Y |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; display all the values in ascending order (by key)
(with-open [cur (sph/cursor env)]
  (run! prn
        (sph/range-query cur "accounts")))


(with-open [cur (sph/cursor env)]
  (into {}
        (sph/range-query cur "accounts")))
;;=> {"admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]},
;;    "user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0},
;;    "user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}}



(with-open [cur (sph/cursor env)]
  (run! prn
        (sph/range-query cur "accounts" :order :desc)))

;; ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]
;; ["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;; ["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]


;;
;; Seek and scan
;;

(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :asc)))
;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]]


(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :desc)))
;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]]


(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :desc :search-type :index-scan-exclusive)))
;;=> [["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]]


;;
;; prefix
;;

(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user" :search-type :prefix)))

;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]]


(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "foo" :search-type :prefix)))
;;=> []


(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user" :search-type :prefix
                         :order :desc)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| T R A N S A C T I O N S |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}

(sph/with-transaction [tx (sph/begin-transaction env)]
  (let [user1 (sph/get-value tx "accounts" "user1")]
    (sph/set-value! tx "accounts" "user1" (update user1 :balance + 150.0))))
;;=> :ok

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 250.0}


(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}

(let [from   "user1"
      to     "user2"
      amount 200.0]
  ;; start transaction
  (sph/with-transaction [tx (sph/begin-transaction env)]
    (let [user1 (sph/get-value tx "accounts" from)
          user2 (sph/get-value tx "accounts" to)]
      (when-not (>= (:balance user1) amount)
        (throw (ex-info (str "Insufficient funds in available from: " from)
                        {:from user1 :amount 200.0 :to user2})))
      (sph/set-value! tx "accounts" "user1" (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" "user2" (update user2 :balance + amount )))))
;;=> :ok

(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 50.0}
