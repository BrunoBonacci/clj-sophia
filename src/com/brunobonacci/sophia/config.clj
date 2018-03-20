(ns com.brunobonacci.sophia.config
  (:require [schema.core :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| C O N F I G U R A T I O N |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def db-config-schema
  {;; name of the database
   :name s/Str

   ;; Database's sequential id number. This number is used in the
   ;; transaction log for the database identification.
   (s/optional-key :id) s/Int

   ;; Set folder to store database data. If variable is not set, it
   ;; will be automatically set as **sophia.path/database_name**.
   (s/optional-key :path) s/Str

   ;; Enable or disable mmap mode.
   (s/optional-key :mmap) s/Int

   ;; Enable or disable O_DIRECT mode.
   (s/optional-key :direct_io) s/Int

   ;; Sync node file on compaction completion.
   (s/optional-key :sync) s/Int

   ;; Enable or disable key expire.
   (s/optional-key :expire) s/Int

   ;; Specify compression driver. Supported: lz4, zstd, none (default).
   (s/optional-key :compression) s/Str

   ;; Scheme field size limit.
   (s/optional-key :limit.field) s/Int

   ;; Total write cache size used for compaction
   ;; (see "memory requirements")
   (s/optional-key :compaction.cache) s/Int

   ;; Set a node file size in bytes. Node file can grow up to two
   ;; times the size before the old node file is being split.
   (s/optional-key :compaction.node_size) s/Int

   ;; Set size of a page to use.
   (s/optional-key :compaction.page_size) s/Int

   ;; Check checksum during compaction.
   (s/optional-key :compaction.page_checksum) s/Int

   ;; Run expire check process every expire_period seconds.
   (s/optional-key :compaction.expire_period) s/Int

   ;; Garbage collection starts when watermark value reaches a certain
   ;; percent of duplicates. When this value reaches a compaction,
   ;; operation is scheduled.
   (s/optional-key :compaction.gc_wm) s/Int

   ;; Check for a gc every gc_period seconds.
   (s/optional-key :compaction.gc_period) s/Int})



(def config-schema
  {;; Set current Sophia environment directory.
   :sophia.path s/Str

   ;; list of DBs to open/create
   :dbs [db-config-schema]

   ;; Set a number of worker threads.
   (s/optional-key :scheduler.threads) s/Int

   ;; Current log sequential number.
   (s/optional-key :metric.lsn) s/Int

   ;; Current transaction sequential number.
   (s/optional-key :metric.tsn) s/Int

   ;; Current node sequential number.
   (s/optional-key :metric.nsn) s/Int

   ;; Current database sequential number.
   (s/optional-key :metric.dsn) s/Int

   ;; Current backup sequential number.
   (s/optional-key :metric.bsn) s/Int

   ;; Current log file sequential number.
   (s/optional-key :metric.lfsn) s/Int

   ;; Enable or disable Write Ahead transaction log.
   (s/optional-key :log.enable) s/Int

   ;; Set folder for transaction log directory. If variable is not
   ;; set, it will be automatically set as **sophia.path/log**.
   (s/optional-key :log.path) s/Str

   ;; Sync transaction log on every commit.
   (s/optional-key :log.sync) s/Int

   ;; Create new log file after rotate_wm updates.
   (s/optional-key :log.rotate_wm) s/Int

   ;; Sync log file on every rotation.
   (s/optional-key :log.rotate_sync) s/Int})



(defn- environment-defaults
  [config]
  config)



(defn- database-defaults [db-config]
  (merge {:mmap 1}
         db-config))



(defn conform-config
  [config]
  (-> config
      ;; turns a list of db-names into configuration maps
      (update :dbs (partial mapv (fn [db] (if (map? db) db {:name db}))))
      ;; apply environment defaults
      (environment-defaults)
      ;; apply database defaults
      (update :dbs (partial mapv database-defaults))
      ;; validate config
      ((partial s/validate config-schema))))



(defn native-config
  [config]
  (let [dbs (:dbs config)]
    (concat
     (map (fn [[k v]] [(name k) v]) (dissoc config :dbs))
     (mapcat #(let [n (:name %)]
                (map (fn [[k v]]
                       [(if (= :name k)
                          (str "db")
                          (str "db." n "."(name k))) v]) %)) dbs))))




(comment
  (->> {:sophia.path "/tmp/sophia-test"
        :dbs ["accounts" {:name "transactions"}]}
       (conform-config)
       (native-config))
  )
