(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))


(defn- uuid []
  (str (java.util.UUID/randomUUID)))



(defn- db-error [sophia-env db message-fmt & values]
  (ex-info (apply format message-fmt values)
           {:sophia sophia-env
            :db db}))



(let [env-refs (atom {})]

  (defn- env* [id]
    (get @env-refs id))

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
       :config config})))



(defn set-value!
  [{:keys [env] :as sophia} db key value]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)]
      (n/sp_setstring doc* "key" key)
      (n/sp_setbytes  doc* "value" (nippy/freeze value))
      (n/op (env* env) (n/sp_set db* doc*))
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn get-value
  [{:keys [env] :as sophia} db key]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          v*   (n/sp_get db* doc*)]
      (n/with-ref v*
        (nippy/thaw (n/sp_getbytes v* "value"))))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn delete-key!
  [{:keys [env] :as sophia} db key]
  (if-let [db* (n/sp_getobject (env* env) (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          _    (n/op (env* env) (n/sp_delete db* doc*))]
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))



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



(defn- -range-query
  [{:keys [doc cursor]}]
  (let [cursor* (cursor-ref cursor)
        _       (when-not cursor* (throw (ex-info "Cursor already closed." {})))
        doc*    (n/sp_get cursor* doc)]
    (lazy-seq
     (when doc*
       (let [key   (n/sp_getstring doc* "key")
             value (nippy/thaw (n/sp_getbytes doc* "value"))]
         (cons [key value]
               (-range-query {:doc doc* :cursor cursor})))))))



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

      (-range-query {:doc doc* :cursor cursor}))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(comment

  (def sph
    (sophia {:sophia.path "/tmp/sophia-test"
             :db "test"}))

  (with-open [cur (cursor sph)]
    (run! prn
          (range-query cur "test")))

  (with-open [cur (cursor sph)]
    (doall (range-query cur "test")))


  (set-value! sph "test" "name" "John")

  (get-value   sph "test" "name")

  (delete-key!  sph "test" "name")

  (set-value! sph "test" "name"
              {:firstname "John" :lastname "Smith" :age 34})

  (get-value  sph "test" "name")


  (doseq [k (take 1000 (repeatedly #(str "test-" (uuid))))]
    (set-value! sph "test" k {:value (uuid) :id (uuid)}))

)
