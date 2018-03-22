(ns com.brunobonacci.sophia.stats
  (:require [clojure.string :as str]))


(def configurable-keys
  ;; configurable key                  type, read-only? description
  '[;; Sophia Environment
    :sophia.version                    [string, ro] "Get current Sophia version."
    :sophia.version_storage            [string, ro] "Get current Sophia storage version."
    :sophia.build                      [string, ro] "Get git commit id of a current build."
    :sophia.status                     [string, ro] "Get Sophia status (eg. online)."
    :sophia.errors                     [int,    ro] "Get a number of errors."
    :sophia.error                      [string, ro] "Get last error description."
    :sophia.path                       [string    ] "Set current Sophia environment directory."
    :sophia.on_log                     [function  ] "Set log function."
    :sophia.on_log_arg                 [string    ] "Set log function argument."

    ;; Scheduler
    :scheduler.threads                 [int       ] "Set a number of worker threads."
    :scheduler.$id.trace               [string, ro] "Get a worker trace per thread."

    ;; Transaction Manager
    :transaction.online_rw             [int,    ro] "Number of active RW transactions."
    :transaction.online_ro             [int,    ro] "Number of active RO transactions."
    :transaction.commit                [int,    ro] "Total number of completed transactions."
    :transaction.rollback              [int,    ro] "Total number of transaction rollbacks."
    :transaction.conflict              [int,    ro] "Total number of transaction conflicts."
    :transaction.lock                  [int,    ro] "Total number of transaction locks."
    :transaction.latency               [string, ro] "Average transaction latency from begin till commit."
    :transaction.log                   [string, ro] "Average transaction log length."
    :transaction.vlsn                  [int,    ro] "Current VLSN."
    :transaction.gc                    [int,    ro] "SSI GC queue size."

    ;; Storage Engine Metrics
    :metric.lsn                        [int       ] "Current log sequential number."
    :metric.tsn                        [int       ] "Current transaction sequential number."
    :metric.nsn                        [int       ] "Current node sequential number."
    :metric.dsn                        [int       ] "Current database sequential number."
    :metric.bsn                        [int       ] "Current backup sequential number."
    :metric.lfsn                       [int       ] "Current log file sequential number."

    ;; Write Ahead Log
    :log.enable                        [int       ] "Enable or disable transaction log."
    :log.path                          [string    ] "Set folder for transaction log directory. If variable is not set, it will be automatically set as **sophia.path/log**."
    :log.sync                          [int       ] "Sync transaction log on every commit."
    :log.rotate_wm                     [int       ] "Create new log file after rotate_wm updates."
    :log.rotate_sync                   [int       ] "Sync log file on every rotation."
    :log.rotate                        [function  ] "Force to rotate log file."
    :log.gc                            [function  ] "Force to garbage-collect log file pool."
    :log.files                         [int,    ro] "Number of log files in the pool."


    ;; Databases
    :db.$name.name                     [string, ro] "Get database name"
    :db.$name.id                       [int       ] "Database's sequential id number. This number is used in the transaction log for the database identification."
    :db.$name.path                     [string    ] "Set folder to store database data. If variable is not set, it will be automatically set as **sophia.path/database_name**."
    :db.$name.mmap                     [int       ] "Enable or disable mmap mode."
    :db.$name.direct_io                [int       ] "Enable or disable O_DIRECT mode."
    :db.$name.sync                     [int       ] "Sync node file on compaction completion."
    :db.$name.expire                   [int       ] "Enable or disable key expire."
    :db.$name.compression              [string    ] "Specify compression driver. Supported: lz4, zstd, none (default)."
    :db.$name.comparator               [function  ] "Set custom comparator function (example: [comparator.c](https://github.com/pmwkaa/sophia/blob/master/example/comparator.c))."
    :db.$name.comparator_arg           [string    ] "Set custom comparator function arg."
    :db.$name.upsert                   [function  ] "Set upsert callback function."
    :db.$name.upsert_arg               [string    ] "Set upsert function argument."
    :db.$name.limit.key                [int,    ro] "Scheme key size limit."
    :db.$name.limit.field              [int       ] "Scheme field size limit."
    :db.$name.index.memory_used        [int,    ro] "Memory used by database for in-memory key indexes in bytes."
    :db.$name.index.size               [int,    ro] "Sum of nodes size in bytes (compressed). This is equal to the full database size."
    :db.$name.index.size_uncompressed  [int,    ro] "Full database size before the compression."
    :db.$name.index.count              [int,    ro] "Total number of keys stored in database. This includes transactional duplicates and not yet-merged duplicates."
    :db.$name.index.count_dup          [int,    ro] "Total number of transactional duplicates."
    :db.$name.index.read_disk          [int,    ro] "Number of disk reads since start."
    :db.$name.index.read_cache         [int,    ro] "Number of cache reads since start."
    :db.$name.index.node_count         [int,    ro] "Number of active nodes."
    :db.$name.index.page_count         [int,    ro] "Total number of pages."

    ;; Database Compaction settings
    :db.$name.compaction.cache         [int       ] "Total write cache size used for compaction (see [memory requirements](../admin/memory_requirements.md))."
    :db.$name.compaction.node_size     [int       ] "Set a node file size in bytes. Node file can grow up to two times the size before the old node file is being split."
    :db.$name.compaction.page_size     [int       ] "Set size of a page to use."
    :db.$name.compaction.page_checksum [int       ] "Check checksum during compaction."
    :db.$name.compaction.expire_period [int       ] "Run expire check process every expire_period seconds."
    :db.$name.compaction.gc_wm         [int       ] "Garbage collection starts when watermark value reaches a certain percent of duplicates. When this value reaches a compaction, operation is scheduled."
    :db.$name.compaction.gc_period     [int       ] "Check for a gc every gc_period seconds."

    ;; Database Performance
    :db.$name.stat.documents_used      [int,    ro] "Memory used by allocated document."
    :db.$name.stat.documents           [int,    ro] "Number of currently allocated document."
    :db.$name.stat.field               [string, ro] "Average field size."
    :db.$name.stat.set                 [int,    ro] "Total number of Set operations."
    :db.$name.stat.set_latency         [string, ro] "Average Set latency."
    :db.$name.stat.delete              [int,    ro] "Total number of Delete operations."
    :db.$name.stat.delete_latency      [string, ro] "Average Delete latency."
    :db.$name.stat.upsert              [int,    ro] "Total number of Upsert operations."
    :db.$name.stat.upsert_latency      [string, ro] "Average Upsert latency."
    :db.$name.stat.get                 [int,    ro] "Total number of Get operations."
    :db.$name.stat.get_latency         [string, ro] "Average Get latency."
    :db.$name.stat.get_read_disk       [string, ro] "Average disk reads by Get operation."
    :db.$name.stat.get_read_cache      [string, ro] "Average cache reads by Get operation."
    :db.$name.stat.pread               [int,    ro] "Total number of pread operations."
    :db.$name.stat.pread_latency       [string, ro] "Average pread latency."
    :db.$name.stat.cursor              [int,    ro] "Total number of Cursor operations."
    :db.$name.stat.cursor_latency      [string, ro] "Average Cursor latency."
    :db.$name.stat.cursor_read_disk    [string, ro] "Average disk reads by Cursor operation."
    :db.$name.stat.cursor_read_cache   [string, ro] "Average cache reads by Cursor operation."
    :db.$name.stat.cursor_ops          [string, ro] "Average number of keys read by Cursor operation."

    ;; Database Scheduler
    :db.$name.scheduler.gc             [int,    ro] "Shows if gc operation is in progress."
    :db.$name.scheduler.expire         [int,    ro] "Shows if expire operation is in progress."
    :db.$name.scheduler.backup         [int,    ro] "Shows if backup operation is in progress."
    ])



(def stats-keys
  (->> (partition 3 configurable-keys)
       (remove (fn [[k [t ro] d]] (= t 'function)))
       (remove (fn [[k [t ro] d]] (str/ends-with? (name k) "_arg")))
       (map (fn [[k [t ro] d]]
              (let [v {:type t :parametric (str/includes? (name k) "$")}]
                [(keyword (str/replace (name k) #"\$[^.]+\." "")) v])))
       (into {})))



(defn std-key
  [key]
  (let [k (name key)]
    (keyword
     (cond
       (str/starts-with? k "db.") (str/replace k #"^db\.[^.]+\." "db.")
       (re-matches #"scheduler\.\d\.trace" k) "scheduler.trace"
       :else k))))
