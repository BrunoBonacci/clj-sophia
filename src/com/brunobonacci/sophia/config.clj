(ns com.brunobonacci.sophia.config
  (:require [schema.core :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| C O N F I G U R A T I O N |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- default-config
  "returns a map with the default general configuration values.
   Because some of the defaults depends on values which the user
   enters this is a function of the user's config."
  [cfg]
  {;; Set current Sophia environment directory.
   ;; REQUIRED
   ;; :sophia.path "/path/to/data"

   ;; list of DBs to open/create
   ;; REQUIRED / NON EMPTY
   ;; every db item must be a simple db name or a map which can be configured
   ;; as described in the following section.
   ;; :dbs ["db1" {:name "db2"}]

   ;; Set a number of worker threads.
   ;; :scheduler.threads

   ;; Current log sequential number.
   ;; :metric.lsn

   ;; Current transaction sequential number.
   ;; :metric.tsn

   ;; Current node sequential number.
   ;; :metric.nsn

   ;; Current database sequential number.
   ;; :metric.dsn

   ;; Current backup sequential number.
   ;; :metric.bsn

   ;; Current log file sequential number.
   ;; :metric.lfsn

   ;; Enable or disable Write Ahead transaction log.
   ;; 0 - disabled, 1 - enabled
   ;; :log.enable

   ;; Set folder for transaction log directory. If variable is not
   ;; set, it will be automatically set as **${sophia.path}/_tx-log**.
   :log.path (str (:sophia.path cfg) "/_tx-log")

   ;; Sync transaction log on every commit.
   ;; 0 - No, 1 - Yes
   ;; :log.sync

   ;; Create new log file after rotate_wm updates.
   ;; :log.rotate_wm

   ;; Sync log file on every rotation.
   ;; 0 - No, 1 - Yes
   ;; :log.rotate_sync

   ;;
   ;; --= driver's properties ==-
   ;;
   :driver
   {;; Tracking settings
    :tracking {;; which native metrics should be tracked via TrackIt
               ;; It accept a function which takes 1 argument (the metric key)
               ;; and it returns true or false whether the metric should be
               ;; tracked or not. The tracking is initiated at environment
               ;; creation.
               ;; Default: track all metrics
               ;; Possible options:
               ;; :all  - track all
               ;; :none - track none
               ;; #(not (str/starts-with (name %1) "db.")) - track everything
               ;;     excluding db specific metrics.
               :track-native :all
               }
    }
   })



(defn- default-db-config
  "returns a map with the default db configuration values.
   Because some of the defaults depends on values which the user
   enters this is a function of the user's config."
  [cfg]
  { ;; name of the database
   ;; REQUIRED
   ;; :name

   ;; Database's sequential id number. This number is used in the
   ;; transaction log for the database identification.
   ;; :id

   ;; Set folder to store database data. If variable is not set, it
   ;; will be automatically set as **${sophia.path}/database_name**.
   ;; :path

   ;; Enable or disable mmap mode.
   :mmap 1

   ;; Enable or disable O_DIRECT mode.
   ;; 0 - disabled, 1 - enabled
   ;; :direct_io

   ;; Sync node file on compaction completion.
   ;; 0 - No, 1 - Yes
   ;; :sync

   ;; Enable or disable key expire.
   ;; 0 - disabled, 1 - enabled
   ;; :expire

   ;; Specify compression driver. Supported: lz4, zstd, none (default).
   ;; :compression "none"

   ;; Scheme field size limit.
   ;; :limit.field

   ;; Total write cache size used for compaction
   ;; (see "memory requirements")
   ;; :compaction.cache

   ;; Set a node file size in bytes. Node file can grow up to two
   ;; times the size before the old node file is being split.
   ;; :compaction.node_size

   ;; Set size of a page to use.
   ;; :compaction.page_size

   ;; Check checksum during compaction.
   ;; 0 - No, 1 - Yes
   ;; :compaction.page_checksum

   ;; Run expire check process every expire_period seconds.
   ;; :compaction.expire_period

   ;; Garbage collection starts when watermark value reaches a certain
   ;; percent of duplicates. When this value reaches a compaction,
   ;; operation is scheduled.
   ;; :compaction.gc_wm

   ;; Check for a gc every gc_period seconds.
   ;; :compaction.gc_period
   })



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
   (s/optional-key :mmap) (s/both s/Int (s/enum 0 1))

   ;; Enable or disable O_DIRECT mode.
   (s/optional-key :direct_io) (s/both s/Int (s/enum 0 1))

   ;; Sync node file on compaction completion.
   (s/optional-key :sync) (s/both s/Int (s/enum 0 1))

   ;; Enable or disable key expire.
   (s/optional-key :expire) (s/both s/Int (s/enum 0 1))

   ;; Specify compression driver. Supported: lz4, zstd, none (default).
   (s/optional-key :compression) (s/both s/Str (s/enum "lz4" "zstd" "none"))

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
   (s/optional-key :compaction.page_checksum) (s/both s/Int (s/enum 0 1))

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
   (s/optional-key :log.enable) (s/both s/Int (s/enum 0 1))

   ;; Set folder for transaction log directory. If variable is not
   ;; set, it will be automatically set as **sophia.path/log**.
   (s/optional-key :log.path) s/Str

   ;; Sync transaction log on every commit.
   (s/optional-key :log.sync) (s/both s/Int (s/enum 0 1))

   ;; Create new log file after rotate_wm updates.
   (s/optional-key :log.rotate_wm) s/Int

   ;; Sync log file on every rotation.
   (s/optional-key :log.rotate_sync) (s/both s/Int (s/enum 0 1))

   ;;
   ;; --= driver's properties ==-
   ;;
   :driver
   {;; Tracking settings
    :tracking {;; which native metrics should be tracked via TrackIt
               :track-native (s/either
                              (s/enum :all :none)
                              (s/pred fn? "track-native must be one of: :all, :none, or a function"))
               }
    }
   })



(defn deep-merge
  "Like merge, but merges maps recursively."
  [& maps]
  (let [maps (filter (comp not nil?) maps)]
    (if (every? map? maps)
      (apply merge-with deep-merge maps)
      (last maps))))



(defn- environment-defaults
  [config]
  (deep-merge (default-config config)
              config))



(defn- database-defaults [db-config]
  (merge (default-db-config db-config)
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
     (map (fn [[k v]] [(name k) v]) (dissoc config :dbs :driver))
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
