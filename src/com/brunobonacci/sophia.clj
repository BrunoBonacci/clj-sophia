(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))

(defn sophia
  [config]
  {:pre [(:sophia.path config) (:db config)]}
  (let [env (n/sp_env)]
    (doseq [[k v] config]
      (n/sp_setstring env (name k) v))
    (n/op env (n/sp_open env))
    {:env env
     :config config}))


(defn- db-error [sophia-env db message-fmt & values]
  (ex-info (apply format message-fmt values)
           {:sophia sophia-env
            :db db}))



(defn set-value!
  [{:keys [env] :as sophia} db key value]
  (if-let [db* (n/sp_getobject env (str "db." db))]
    (let [doc* (n/sp_document db*)]
      (n/sp_setstring doc* "key" key)
      (n/sp_setbytes  doc* "value" (nippy/freeze value))
      (n/op env (n/sp_set db* doc*))
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn get-value
  [{:keys [env] :as sophia} db key]
  (if-let [db* (n/sp_getobject env (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          v*   (n/sp_get db* doc*)]
      (n/with-ref v*
        (nippy/thaw (n/sp_getbytes v* "value"))))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn delete-key!
  [{:keys [env] :as sophia} db key]
  (if-let [db* (n/sp_getobject env (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          _    (n/op env (n/sp_delete db* doc*))]
      :ok)
    (throw
     (db-error sophia db "Database %s not found!" db))))



(defn- -range-query
  [{:keys [doc cursor]}]
  (let [doc*  (n/sp_get cursor doc)]
    (when doc*
      (let [key   (n/sp_getstring doc* "key")
            value (nippy/thaw (n/sp_getbytes doc* "value"))]
        (lazy-seq
         (cons [key value]
               (-range-query {:doc doc* :cursor cursor})))))))



(defn range-query
  [{:keys [env] :as sophia} db
   & {:keys [key order search-type] :as opts
      :or {order :asc
           search-type :index-scan-inclusive}}]
  {:pre [(#{:asc :desc} order)
         (#{:prefix :index-scan-inclusive
            :index-scan-exclusive} search-type)]}
  (if-let [db*  (n/sp_getobject env (str "db." db))]
    (let [doc*  (n/sp_document db*)
          cur*  (n/sp_cursor env)
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

      (-range-query {:doc doc* :cursor cur*}))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(comment

  (def sph
    (sophia {:sophia.path "/tmp/test3"
             :db "test"}))

  (range-query sph "test" :order :asc
               :key "test-05"
               :search-type :index-scan-inclusive)

  (set-value! sph "test" "name" "John")

  (get-value   sph "test" "name")

  (delete-key!  sph "test" "name")

  (set-value! sph "test" "name"
              {:firstname "John" :lastname "Smith" :age 34})

  (get-value  sph "test" "name")


  (defn uuid [] (str (java.util.UUID/randomUUID)))

  (doseq [k (take 1000 (repeatedly #(str "test-" (uuid))))]
    (set-value! sph "test" k {:value (uuid) :id (uuid)}))

)
