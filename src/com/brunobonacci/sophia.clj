(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]
            [taoensso.nippy :as nippy]))

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
          v*   (n/op env (n/sp_get db* doc*))]
      (when v*
        (nippy/thaw (n/sp_getbytes v* "value"))))
    (throw
     (db-error sophia db "Database %s not found!" db))))



(comment

  (def sph
    (sophia {:sophia.path "/tmp/test3"
             :db "test"}))

  (set-value! sph "test" "name" "John")

  (get-value  sph "test" "name")

  (set-value! sph "test" "name" {:firstname "John" :lastname "Smith" :age 34})

  (get-value  sph "test" "name")

)
