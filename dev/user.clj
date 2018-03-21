(ns user
  (:require [com.brunobonacci.sophia :as sph]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                ----==| G E T / S E T / D E L E T E |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def env (sph/sophia {:sophia.path "/tmp/sophia-test"
                      :dbs ["accounts", {:name "transactions"}]}))


;; set a simple value
(sph/set-value!  env "accounts" "user1" "John")
;;=> "John"

;; get the value back
(sph/get-value   env "accounts" "user1")
;;=> "John"

;; delete a key
(sph/delete-key! env "accounts" "user1")
;;=> nil

;; now the key isn't present
(sph/get-value   env "accounts" "user1")
;;=> nil

;; set a complex value
(sph/set-value! env "accounts" "user1"
                {:firstname "John" :lastname "Doe" :age 34 :balance 100.0})
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}

;; get it back
(sph/get-value   env "accounts" "user1")
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}

(sph/set-value! env "accounts" "user2"
                {:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0})
;;=> :{:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0}

(sph/set-value! env "accounts" "admin1"
                {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]})
;;=> {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]}



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
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 250.0}

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
      ;; if funds are not available abort the transaction
      (when-not (>= (:balance user1) amount)
        (throw (ex-info (str "Insufficient funds in available from: " from)
                        {:from user1 :amount amount :to user2})))
      ;; these two update are inside a transaction and they will be stored
      ;; atomically
      (sph/set-value! tx "accounts" from (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" to   (update user2 :balance + amount )))))
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 50.0}


;; multi table transactions
(require '[clj-time.core :as t]
         '[clj-time.format :as f])


(defn now-UTC
  "Returns the current UTC timestamp in ISO format"
  []
  (f/unparse (f/formatters :date-time) (t/now)))


(now-UTC)
;;=> "2018-03-21T22:15:52.512Z"

(let [from   "user2"
      to     "user1"
      amount 250.0]
  ;; start transaction
  (sph/with-transaction [tx (sph/begin-transaction env)]
    (let [user1 (sph/get-value tx "accounts" from)
          user2 (sph/get-value tx "accounts" to)]
      (when-not (>= (:balance user1) amount)
        (throw (ex-info (str "Insufficient funds in available from: " from)
                        {:from user1 :amount amount :to user2})))
      ;; add a entry in the `transactions`
      (sph/set-value! tx "transactions" (now-UTC)
                      {:tx-type :transfer :from from :amount amount :to to})
      ;; now update both user's balance
      (sph/set-value! tx "accounts" from (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" to   (update user2 :balance + amount )))))


(with-open [cur (sph/cursor env)]
  (into [] (sph/range-query cur "transactions")))
;;=> [["2018-03-21T22:26:16.284Z" {:tx-type :transfer, :from "user2", :amount 250.0, :to "user1"}]]

(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 150.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 300.0}
