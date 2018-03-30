(ns com.brunobonacci.sophia.stats
  (:require [clojure.string :as str]
            [com.brunobonacci.sophia.native :as n]))



(def system-keys
  [;; Sophia Environment
   {:key :sophia.version,         :type :string,   :read-only? true,  :description "Get current Sophia version."}
   {:key :sophia.version_storage, :type :string,   :read-only? true,  :description "Get current Sophia storage version."}
   {:key :sophia.build,           :type :string,   :read-only? true,  :description "Get git commit id of a current build."}
   {:key :sophia.status,          :type :string,   :read-only? true,  :description "Get Sophia status (eg. online)."}
   {:key :sophia.errors,          :type :int,      :read-only? true,  :description "Get a number of errors."}
   {:key :sophia.path,            :type :string,   :read-only? false, :description "Set current Sophia environment directory."}
   {:key :sophia.on_log,          :type :function, :read-only? false, :description "Set log function."}
   {:key :sophia.on_log_arg,      :type :string,   :read-only? false, :description "Set log function argument."}

   ;; Scheduler
   {:key :scheduler.threads,      :type :int,      :read-only? false, :description "Set a number of worker threads."}
   {:group :scheduler,  :key :trace,    :type :string,   :read-only? true,  :parametrized-by "scheduler.threads", :prefix "scheduler", :suffix "trace", :description "Get a worker trace per thread."}

   ;; Transaction Manager
   {:key :transaction.online_rw,  :type :int,      :read-only? true,  :description "Number of active RW transactions."}
   {:key :transaction.online_ro,  :type :int,      :read-only? true,  :description "Number of active RO transactions."}
   {:key :transaction.commit,     :type :int,      :read-only? true,  :description "Total number of completed transactions."}
   {:key :transaction.rollback,   :type :int,      :read-only? true,  :description "Total number of transaction rollbacks."}
   {:key :transaction.conflict,   :type :int,      :read-only? true,  :description "Total number of transaction conflicts."}
   {:key :transaction.lock,       :type :int,      :read-only? true,  :description "Total number of transaction locks."}
   {:key :transaction.latency,    :type :string,   :read-only? true,  :description "Average transaction latency from begin till commit." :format :latency}
   {:key :transaction.log,        :type :string,   :read-only? true,  :description "Average transaction log length." :format :triplet}
   {:key :transaction.vlsn,       :type :int,      :read-only? true,  :description "Current VLSN."}
   {:key :transaction.gc,         :type :int,      :read-only? true,  :description "SSI GC queue size."}

   ;; Storage Engine Metrics
   {:key :metric.lsn,             :type :int,      :read-only? false, :description "Current log sequential number."}
   {:key :metric.tsn,             :type :int,      :read-only? false, :description "Current transaction sequential number."}
   {:key :metric.nsn,             :type :int,      :read-only? false, :description "Current node sequential number."}
   {:key :metric.dsn,             :type :int,      :read-only? false, :description "Current database sequential number."}
   {:key :metric.bsn,             :type :int,      :read-only? false, :description "Current backup sequential number."}
   {:key :metric.lfsn,            :type :int,      :read-only? false, :description "Current log file sequential number."}

   ;; Write Ahead Log
   {:key :log.enable,             :type :int,      :read-only? false, :description "Enable or disable transaction log."}
   {:key :log.path,               :type :string,   :read-only? false, :description "Set folder for transaction log directory. If variable is not set, it will be automatically set as **sophia.path/log**."}
   {:key :log.sync,               :type :int,      :read-only? false, :description "Sync transaction log on every commit."}
   {:key :log.rotate_wm,          :type :int,      :read-only? false, :description "Create new log file after rotate_wm updates."}
   {:key :log.rotate_sync,        :type :int,      :read-only? false, :description "Sync log file on every rotation."}
   {:key :log.rotate,             :type :function, :read-only? false, :description "Force to rotate log file."}
   {:key :log.gc,                 :type :function, :read-only? false, :description "Force to garbage-collect log file pool."}
   {:key :log.files,              :type :int,      :read-only? true,  :description "Number of log files in the pool."}
   ])



(def database-keys
  [{:group :db, :key :name,                     :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "name",                     :description "Get database name"}
   {:group :db, :key :id,                       :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "id",                       :description "Database's sequential id number. This number is used in the transaction log for the database identification."}
   {:group :db, :key :path,                     :type :string,   :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "path",                     :description "Set folder to store database data. If variable is not set, it will be automatically set as **sophia.path/database_name**."}
   {:group :db, :key :mmap,                     :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "mmap",                     :description "Enable or disable mmap mode."}
   {:group :db, :key :direct_io,                :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "direct_io",                :description "Enable or disable O_DIRECT mode."}
   {:group :db, :key :sync,                     :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "sync",                     :description "Sync node file on compaction completion."}
   {:group :db, :key :expire,                   :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "expire",                   :description "Enable or disable key expire."}
   {:group :db, :key :compression,              :type :string,   :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compression",              :description "Specify compression driver. Supported: lz4, zstd, none (default)."}
   {:group :db, :key :comparator,               :type :function, :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "comparator",               :description "Set custom comparator function (example: [comparator.c](https://github.com/pmwkaa/sophia/blob/master/example/comparator.c))."}
   {:group :db, :key :comparator_arg,           :type :string,   :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "comparator_arg",           :description "Set custom comparator function arg."}
   {:group :db, :key :upsert,                   :type :function, :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "upsert",                   :description "Set upsert callback function."}
   {:group :db, :key :upsert_arg,               :type :string,   :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "upsert_arg",               :description "Set upsert function argument."}
   {:group :db, :key :limit.key,                :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "limit.key",                :description "Scheme key size limit."}
   {:group :db, :key :limit.field,              :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "limit.field",              :description "Scheme field size limit."}
   {:group :db, :key :index.memory_used,        :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.memory_used",        :description "Memory used by database for in-memory key indexes in bytes."}
   {:group :db, :key :index.size,               :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.size",               :description "Sum of nodes size in bytes (compressed). This is equal to the full database size."}
   {:group :db, :key :index.size_uncompressed,  :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.size_uncompressed",  :description "Full database size before the compression."}
   {:group :db, :key :index.count,              :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.count",              :description "Total number of keys stored in database. This includes transactional duplicates and not yet-merged duplicates."}
   {:group :db, :key :index.count_dup,          :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.count_dup",          :description "Total number of transactional duplicates."}
   {:group :db, :key :index.read_disk,          :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.read_disk",          :description "Number of disk reads since start."}
   {:group :db, :key :index.read_cache,         :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.read_cache",         :description "Number of cache reads since start."}
   {:group :db, :key :index.node_count,         :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.node_count",         :description "Number of active nodes."}
   {:group :db, :key :index.page_count,         :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "index.page_count",         :description "Total number of pages."}
   {:group :db, :key :compaction.cache,         :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.cache",         :description "Total write cache size used for compaction (see [memory requirements](../admin/memory_requirements.md))."}
   {:group :db, :key :compaction.node_size,     :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.node_size",     :description "Set a node file size in bytes. Node file can grow up to two times the size before the old node file is being split."}
   {:group :db, :key :compaction.page_size,     :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.page_size",     :description "Set size of a page to use."}
   {:group :db, :key :compaction.page_checksum, :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.page_checksum", :description "Check checksum during compaction."}
   {:group :db, :key :compaction.expire_period, :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.expire_period", :description "Run expire check process every expire_period seconds."}
   {:group :db, :key :compaction.gc_wm,         :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.gc_wm",         :description "Garbage collection starts when watermark value reaches a certain percent of duplicates. When this value reaches a compaction, operation is scheduled."}
   {:group :db, :key :compaction.gc_period,     :type :int,      :read-only? false, :parametrized-by "db.name", :prefix "db", :suffix "compaction.gc_period",     :description "Check for a gc every gc_period seconds."}
   {:group :db, :key :stat.documents_used,      :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.documents_used",      :description "Memory used by allocated document."}
   {:group :db, :key :stat.documents,           :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.documents",           :description "Number of currently allocated document."}
   {:group :db, :key :stat.field,               :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.field",               :description "Average field size." :format :triplet}
   {:group :db, :key :stat.set,                 :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.set",                 :description "Total number of Set operations."}
   {:group :db, :key :stat.set_latency,         :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.set_latency",         :description "Average Set latency." :format :latency}
   {:group :db, :key :stat.delete,              :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.delete",              :description "Total number of Delete operations."}
   {:group :db, :key :stat.delete_latency,      :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.delete_latency",      :description "Average Delete latency." :format :latency}
   {:group :db, :key :stat.upsert,              :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.upsert",              :description "Total number of Upsert operations."}
   {:group :db, :key :stat.upsert_latency,      :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.upsert_latency",      :description "Average Upsert latency." :format :latency}
   {:group :db, :key :stat.get,                 :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.get",                 :description "Total number of Get operations."}
   {:group :db, :key :stat.get_latency,         :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.get_latency",         :description "Average Get latency." :format :latency}
   {:group :db, :key :stat.get_read_disk,       :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.get_read_disk",       :description "Average disk reads by Get operation." :format :latency}
   {:group :db, :key :stat.get_read_cache,      :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.get_read_cache",      :description "Average cache reads by Get operation." :format :latency}
   {:group :db, :key :stat.pread,               :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.pread",               :description "Total number of pread operations."}
   {:group :db, :key :stat.pread_latency,       :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.pread_latency",       :description "Average pread latency." :format :latency}
   {:group :db, :key :stat.cursor,              :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.cursor",              :description "Total number of Cursor operations."}
   {:group :db, :key :stat.cursor_latency,      :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.cursor_latency",      :description "Average Cursor latency." :format :latency}
   {:group :db, :key :stat.cursor_read_disk,    :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.cursor_read_disk",    :description "Average disk reads by Cursor operation." :format :latency}
   {:group :db, :key :stat.cursor_read_cache,   :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.cursor_read_cache",   :description "Average cache reads by Cursor operation." :format :latency}
   {:group :db, :key :stat.cursor_ops,          :type :string,   :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "stat.cursor_ops",          :description "Average number of keys read by Cursor operation." :format :latency}
   {:group :db, :key :scheduler.gc,             :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "scheduler.gc",             :description "Shows if gc operation is in progress."}
   {:group :db, :key :scheduler.expire,         :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "scheduler.expire",         :description "Shows if expire operation is in progress."}
   {:group :db, :key :scheduler.backup,         :type :int,      :read-only? true,  :parametrized-by "db.name", :prefix "db", :suffix "scheduler.backup",         :description "Shows if backup operation is in progress."}
   ])



(defn -metric-value
  "Internal-use: returns the current value of a metric if available, nil otherwise."
  [env* metric-name type]
  (case type
    :string (n/c-string->jvm (n/sp_getstring env* metric-name))
    :int    (as-> (n/sp_getint env* metric-name) $ (if (= $ -1) nil $))
    (throw (ex-info "Unsupported metric type"
                    {:metric-name metric-name :type type}))))



(defn parse-double
  [num]
  (when num
    (Double/parseDouble num)))



(defn micros->millis
  [time]
  (when time
    (/ time 1000)))



(defn parse-triplet
  [val]
  (when val
    (->> (str/split val #" ")
         (map parse-double)
         (zipmap [:min :max :avg]))))



(defn parse-latency
  [lantency]
  (when lantency
    (->> (str/split lantency #" ")
         (map (comp micros->millis parse-double))
         (zipmap [:min :max :avg]))))



(defn available-keys
  [env* db-names]
  (let [scheduler-keys (filter #(= :scheduler (:group %)) system-keys)]
    (->>
     (concat
      ;; database keys
      (for [k  database-keys
          db db-names]
        (let [{:keys [prefix suffix]} k]
          (assoc k :key (keyword (str/join "." [prefix db suffix])))))
      ;; scheduler keys
      (for [k  scheduler-keys
          t  (range (-metric-value env* "scheduler.threads" :int))]
        (let [{:keys [prefix suffix]} k]
          (assoc k :key (keyword (str/join "." [prefix t suffix]))))))
     ;; all other system keys
     (concat (remove :group system-keys))
     (map (juxt :key identity))
     (into {}))))
