(ns com.brunobonacci.sophia
  (:require [com.brunobonacci.sophia.native :as n]))



(defn sophia
  [config]
  {:pre [(:sophia.path config) (:db config)]}
  (let [env (n/sp_env)]
    (doseq [[k v] config]
      (n/sp_setstring env (name k) v))
    (n/op env (n/sp_open env))
    {:env env
     :config config}))



(defn set-value!
  [{:keys [env] :as sophia} db key value]
  (if-let [db* (n/sp_getobject env (str "db." db))]
    (let [doc* (n/sp_document db*)]
      (n/sp_setstring doc* "key" key)
      (n/sp_setstring doc* "value" value)
      (n/op env (n/sp_set db* doc*))
      :ok)
    (throw (ex-info (format "Database %s not found!" db)
                    {:sophia sophia
                     :db db}))))


(defn get-value
  [{:keys [env] :as sophia} db key]
  (if-let [db* (n/sp_getobject env (str "db." db))]
    (let [doc* (n/sp_document db*)
          _    (n/sp_setstring doc* "key" key)
          v*   (n/op env (n/sp_get db* doc*))]
      (when v*
        (n/sp_getstring v* "value")))
    (throw (ex-info (format "Database %s not found!" db)
                    {:sophia sophia
                     :db db}))))

(comment

  (def sph
    (sophia {:sophia.path "/tmp/test"
             :db "test"}))

  (set-value! sph "test" "name" "John")

  (get-value  sph "test" "name")
  )


(comment

  (def db (n/sp_getobject (:env sph) "db.test"))
  (def o (n/sp_document db))

  (n/sp_setstring o "key" "name")

  (def o (n/sp_get db o))

  (when-not o
    (println "not found"))

  (sp_getstring o "key")

  )
