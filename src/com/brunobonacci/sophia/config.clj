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
   (schema.core/optional-key :id) s/Int

   ;; Set folder to store database data. If variable is not set, it
   ;; will be automatically set as **sophia.path/database_name**.
   (schema.core/optional-key :path) s/Str

   ;; Enable or disable mmap mode.
   (schema.core/optional-key :mmap) s/Int

   ;; Enable or disable O_DIRECT mode.
   (schema.core/optional-key :direct_io) s/Int

   ;; Sync node file on compaction completion.
   (schema.core/optional-key :sync) s/Int

   ;; Enable or disable key expire.
   (schema.core/optional-key :expire) s/Int

   ;; Specify compression driver. Supported: lz4, zstd, none (default).
   (schema.core/optional-key :compression) s/Str

   ;; Scheme field size limit.
   (schema.core/optional-key :limit.field) s/Int

   ;; Total write cache size used for compaction
   ;; (see "memory requirements")
   (schema.core/optional-key :compaction.cache) s/Int

   ;; Set a node file size in bytes. Node file can grow up to two
   ;; times the size before the old node file is being split.
   (schema.core/optional-key :compaction.node_size) s/Int

   ;; Set size of a page to use.
   (schema.core/optional-key :compaction.page_size) s/Int

   ;; Check checksum during compaction.
   (schema.core/optional-key :compaction.page_checksum) s/Int

   ;; Run expire check process every expire_period seconds.
   (schema.core/optional-key :compaction.expire_period) s/Int

   ;; Garbage collection starts when watermark value reaches a certain
   ;; percent of duplicates. When this value reaches a compaction,
   ;; operation is scheduled.
   (schema.core/optional-key :compaction.gc_wm) s/Int

   ;; Check for a gc every gc_period seconds.
   (schema.core/optional-key :compaction.gc_period) s/Int})


(def config-schema
  {;; Set current Sophia environment directory.
   :sophia.path s/Str

   ;; list of DBs to open/create
   :dbs [db-config-schema]

   ;; Set a number of worker threads.
   (schema.core/optional-key :scheduler.threads) s/Int

   ;; Current log sequential number.
   (schema.core/optional-key :metric.lsn) s/Int

   ;; Current transaction sequential number.
   (schema.core/optional-key :metric.tsn) s/Int

   ;; Current node sequential number.
   (schema.core/optional-key :metric.nsn) s/Int

   ;; Current database sequential number.
   (schema.core/optional-key :metric.dsn) s/Int

   ;; Current backup sequential number.
   (schema.core/optional-key :metric.bsn) s/Int

   ;; Current log file sequential number.
   (schema.core/optional-key :metric.lfsn) s/Int

   ;; Enable or disable Write Ahead transaction log.
   (schema.core/optional-key :log.enable) s/Int

   ;; Set folder for transaction log directory. If variable is not
   ;; set, it will be automatically set as **sophia.path/log**.
   (schema.core/optional-key :log.path) s/Str

   ;; Sync transaction log on every commit.
   (schema.core/optional-key :log.sync) s/Int

   ;; Create new log file after rotate_wm updates.
   (schema.core/optional-key :log.rotate_wm) s/Int

   ;; Sync log file on every rotation.
   (schema.core/optional-key :log.rotate_sync) s/Int})
