(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;             ----==| U T I L I T Y   F U N C T I O N S |==----              ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- uuid []
  (str (java.util.UUID/randomUUID)))



(defn- db-error [sophia-env db message-fmt & values]
  (ex-info (apply format message-fmt values)
           {:sophia sophia-env
            :db db}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| T R A N S A C T I O N S |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn- commit-result
  [trxid silent result]
  (if-not silent
    (case result
      :ok :ok
      :rollback (throw
                 (ex-info
                  (format
                   (str "The transaction %s has been rolled back"
                        " because if a concurrent modification.") trxid)
                  {:trx trxid :result result}))
      :lock (throw
             (ex-info
              (format
               (str "The transaction %s has been rolled back"
                    " because another transaction is locking "
                    "the one key.") trxid)
              {:trx trxid :result result})))
    result))




(let [;; Hide native references away to avoid
      ;; misuse and potential JVM crashes.
      env-refs (atom {})
      trx-refs (atom {})]

  ;;
  ;; ACCESSORS
  ;;
  (defn- env* [id]
    (or (get @env-refs id)
       (throw (ex-info "Environment not found or terminated." {:env id}))))


  (defn- trx* [id]
    (or (get @trx-refs id)
       (throw (ex-info "Transaction already terminated." {:trx id}))))


  ;;
  ;; PUBLIC API
  ;;

  (defn sophia
    [config]
    {:pre [(:sophia.path config) (:db config)]}
    (let [env   (n/sp_env)
          envid (uuid)]
      (doseq [[k v] config]
        (n/sp_setstring env (name k) v))
      (n/op env (n/sp_open env))
      (swap! env-refs assoc envid env)
      {:env envid
       :config config}))



  (defn begin-transaction
    [{:keys [env] :as sophia}]
    (let [trx*  (n/sp_begin (env* env))
          trxid (uuid)]
      (swap! trx-refs assoc trxid trx*)
      {:trx trxid :env env :sophia sophia}))



  (defn commit
    [{:keys [env trx] :as transaction} & {:keys [silent]}]
    (let [refs @trx-refs
          ref* (get refs trx)]
      (if (and ref* (compare-and-set! trx-refs refs (dissoc refs trx)))
        (commit-result
         trx silent
         (case (n/op (env* env) (n/sp_commit ref*))
           0 :ok
           1 :rollback
           2 :lock))
        (throw (ex-info "Transaction already terminated." transaction)))))



  (defn rollback
    [{:keys [trx] :as transaction}]
    (let [refs @trx-refs
          ref* (get refs trx)]
      (if (and ref* (compare-and-set! trx-refs refs (dissoc refs trx)))
        (do (n/sp_destroy ref*) nil)
        (throw (ex-info "Transaction already terminated." transaction))))))



(defmacro with-transaction
  "bindings => [name init ...]
  Evaluates body in a transaction. At the end of the block
  the transaction is committed. If an exception occur before
  the transaction is rolled back."
  [bindings & body]
  (assert (vector? bindings) "a vector for its binding")
  (assert (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (let [res#
                                      (with-transaction ~(subvec bindings 2) ~@body)]
                                  ;; commit transaction
                                  (commit ~(bindings 0))
                                  ;; return last value
                                  res#)
                                (catch Throwable x#
                                    (rollback ~(bindings 0))
                                    (throw x#))))
    :else (throw (IllegalArgumentException.
                  "with-transaction only allows Symbols in bindings"))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;            ----==| G E T   /   S E T   /   D E L E T E |==----             ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn set-value!
  [{:keys [env trx] :as sophia} db key value]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)]
      (n/sp_setstring doc* "key" key)
      (n/sp_setbytes  doc* "value" (nippy/freeze value))
      (n/op (env* env) (n/sp_set (if trx (trx* trx) db*) doc*))
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn get-value
  [{:keys [env trx] :as sophia} db key]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          v*   (n/sp_get (if trx (trx* trx) db*) doc*)]
      (n/with-ref v*
        (nippy/thaw (n/sp_getbytes v* "value"))))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn delete-key!
  [{:keys [env trx] :as sophia} db key]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          _    (n/op (env* env) (n/sp_delete (if trx (trx* trx) db*) doc*))]
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;      ----==| C U R S O R   A N D   R A N G E   Q U E R I E S |==----       ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defprotocol ICursor
  (sophia-env [_] "Sophia db configuration and environment")
  (sophia-ref [_] "Sophia native env ref")
  (cursor-ref [_] "Internal cursor ref")
  (close      [_] "to close the cursor when done"))



(deftype Cursor [sophia env-ref cursor]

  ICursor
  (sophia-env [_] sophia)
  (sophia-ref [_] env-ref)
  (cursor-ref [_] @cursor)
  (close [_] (when-let [ref* @cursor]
               (when (compare-and-set! cursor ref* nil)
                 (n/sp_destroy ref*)))))



(defn cursor
  [{:keys [env] :as sophia}]
  (Cursor. sophia (env* env) (atom (n/sp_cursor (env* env)))))



(defn- range-query-iterate
  [{:keys [doc cursor]}]
  (let [cursor* (cursor-ref cursor)
        _       (when-not cursor* (throw (ex-info "Cursor already closed." {})))
        doc*    (n/sp_get cursor* doc)]
    (lazy-seq
     (when doc*
       (let [key   (n/sp_getstring doc* "key")
             value (nippy/thaw (n/sp_getbytes doc* "value"))]
         (cons [key value]
               (range-query-iterate {:doc doc* :cursor cursor})))))))



(defn range-query
  [^Cursor cursor db
   & {:keys [key order search-type] :as opts
      :or {order :asc
           search-type :index-scan-inclusive}}]
  {:pre [(not (nil? cursor))
         (#{:asc :desc} order)
         (#{:prefix :index-scan-inclusive
            :index-scan-exclusive} search-type)]}
  (if-let [db*  (n/sp_getobject (sophia-ref cursor) (str "db." db))]
    (let [doc*  (n/sp_document db*)
          order (if (= :desc order) "<=" ">=")
          order (if (= :index-scan-exclusive search-type)
                  (str/replace order #"=" "")
                  order)
          order (if (= :prefix search-type) ">=" order)]
      (n/sp_setstring doc* "order" order)
      (cond
        (and key (= :prefix search-type))
        (n/sp_setstring doc* "prefix" key)

        key
        (n/sp_setstring doc* "key" key))

      (range-query-iterate {:doc doc* :cursor cursor}))
    (throw
     (db-error sophia db "Database %s not found!" db))))
