(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]
            [com.brunobonacci.sophia.config :as c]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;             ----==| U T I L I T Y   F U N C T I O N S |==----              ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- uuid
  "Returns a random UUID as a string"
  []
  (str (java.util.UUID/randomUUID)))



(defn- db-error
  "return an exception with database info"
  [sophia-env db message-fmt & values]
  (ex-info (apply format message-fmt values)
           {:sophia sophia-env
            :db db}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| T R A N S A C T I O N S |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn- commit-result
  "If the transaction fails, and the silent flag is false
   an exception is raised, otherwise the status is returned."
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
    "Returns a sophia db environment to access the databases"
    [config]
    (let [env    (n/sp_env)
          envid  (uuid)
          config (c/conform-config config)]
      (doseq [[k v] (c/native-config config)]
        (if (string? v)
          (n/sp_setstring env k v)
          (n/sp_setint env k v)))
      (n/op env (n/sp_open env))
      (swap! env-refs assoc envid env)
      {:env envid
       :config config}))



  (defn begin-transaction
    "Begins a transaction for the given sophia database"
    [{:keys [env] :as sophia}]
    (let [trx*  (n/sp_begin (env* env))
          trxid (uuid)]
      (swap! trx-refs assoc trxid trx*)
      {:trx trxid :env env :sophia sophia}))



  (defn commit
    "Commits the transaction for the given database.
     There are three possible outcomes:

        - :ok - the transaction is committed successfully
        - :rollback - the transaction is aborted because
             another transaction completed before this
             and updated the same key(s).
        - :lock - another transaction has a lock on one
             or more keys is pending commit in another
             transaction. It could be retried in a later time.
    "
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
    "It aborts the given transaction and discards all the changes
     associated with it.
    "
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
  "Set the given value to the database. If a transaction is passed as
  `sophia` the value will be set only if the transaction is
  successfully committed.
  It returns the value which for set in.
  "
  [{:keys [env trx] :as sophia} db key value]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)]
      (n/sp_setstring doc* "key" key)
      (n/sp_setbytes  doc* "value" (nippy/freeze value))
      (n/op (env* env) (n/sp_set (if trx (trx* trx) db*) doc*))
      value)
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn get-value
  "Retrieve the current value of a key. If a transaction is passed as
  `sophia` environment the value could have been changed in the
  transaction; in that case the updated value will be returned
  (read your own writes). To read the last committed value
  just use the main sophia environment.
  If the key is not found `nil` is returned unless
  a `default-value` is provided.
  "
  ([{:keys [env trx] :as sophia} db key default-value]
   (or (get-value sophia db key) default-value))
  ([{:keys [env trx] :as sophia} db key]
   (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
     (let [doc* (n/sp_document db*)
           _    (n/sp_setstring doc* "key" key)
           v*   (n/sp_get (if trx (trx* trx) db*) doc*)]
       (n/with-ref v*
         (nippy/thaw (n/sp_getbytes v* "value"))))
     (throw
      (db-error sophia db "Database %s not found!" db)))))



(defn delete-key!
  "It deletes a key. like `set-value!` if a transaction is
  passed the change will only be visible when the transaction
  is committed. It returns `nil`."
  [{:keys [env trx] :as sophia} db key]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          _    (n/op (env* env) (n/sp_delete (if trx (trx* trx) db*) doc*))]
      nil)
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
  "Returns a cursor with a snapshot isolation level consistency
   which means new writes won't affect the query result.
   This ensures that all the reads are consistent in regards
   to the point in time this cursor is created.
   The cursor must be closed (with `.close()`) once terminated.
   Best used with `with-open` Clojure macro.
  "
  [{:keys [env] :as sophia}]
  (Cursor. sophia (env* env) (atom (n/sp_cursor (env* env)))))



(defn- range-query-iterate
  "It returns next item as tuple `[key value]` for the given cursor read."
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
  "Returns a lazy-sequence of all items matching the query.
  The output is a lazy-sequence of tuples if the following
  form `[key value]`. Keys will be ordered according to the
  `:order` option (default `:asc`).

  Three types of query are available:

    * *full table scans* - The database is traversed from the
      beginning towards the end. Keys are alphabetically ordered.
      Order can be reversed via `:order :desc` option.
      example:

        (with-open [cur (sph/cursor env)]
          (into {} (sph/range-query cur \"accounts\")))

    * *seek and scan* - By providing a `key` argument
      you can seek a specific point in the key-range
      and scan forward or backward from that key.
      The direction of the scanning can be controlled
      via the `:order` parameter. Finally you can control
      whether the item matching `key` should be included
      or excluded (default `:index-scan-inclusive`) from
      the search via `:search-type` option.
      example:

        (with-open [cur (sph/cursor env)]
          (into []
            (sph/range-query cur \"accounts\"
                      :key \"user1\" :order :desc)))

    * *prefix* - By providing a `key` argument together
      with `:search-type :prefix` you can provide
      a common prefix for the keys you are searching for.
      Note that for this type of queries the `:order`
      isn't considered at all and just ignored, keys will
      always be returned in ascending order.
      example:

        (with-open [cur (sph/cursor env)]
          (into []
            (sph/range-query cur \"accounts\"
                      :key \"user\" :search-type :prefix)))


  Syntax:

    - *cursor* (required) snapshot isolation
    - *db* (required) the name of the db to run the query onto.
    - *:key* (optional) A key to seek from which to start
         scanning from.
    - *:order* (optional) `:asc` | `:desc` (default: `:asc`)
    - *:search-type* (optional) valid values:
       `:index-scan-inclusive` (default) include the matching key
        if found as part of the result.
       `:index-scan-exclusive` exclude the matching key from the result.
       `:prefix` returns all the keys which start with the `:key` argument

   If none of the keys match the query or the db it is empty an empty
   sequence is returned.
   "
  [^Cursor cursor db
   & {:keys [key order search-type] :as opts
      :or {order       :asc
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
