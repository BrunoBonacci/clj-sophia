# clj-sophia
[![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/clj-sophia.svg)](https://clojars.org/com.brunobonacci/clj-sophia) [![CircleCI (all branches)](https://img.shields.io/circleci/project/github/BrunoBonacci/clj-sophia.svg)](BrunoBonacci)
 ![last-commit](https://img.shields.io/github/last-commit/BrunoBonacci/clj-sophia.svg) [![Dependencies Status](https://jarkeeper.com/BrunoBonacci/safely/status.svg)](https://jarkeeper.com/BrunoBonacci/clj-sophia)


A Clojure driver for [Sophia DB](http://sophia.systems/).

### How does it differ from other storages?

Sophia is RAM-Disk hybrid storage. It is designed to provide best
possible on-disk performance without degradation in time. It has
guaranteed *O(1)* worst case complexity for read, write and range scan
operations.

It adopts to expected write rate, total capacity and cache
size. Memory requirements for common HDD and Flash drives can be seen
[Here](http://sophia.systems/v2.2/admin/memory_requirements.html).

### What is it good for?

For server environment, which requires lowest latency access (both
read and write), predictable behaviour, optimized storage schema and
transaction guarantees.

It can efficiently work with large volumes of ordered data, such as a
time-series, analytics, events, logs, counters, metrics, full-text
search, common key-value, etc.

### Features

* Full ACID compliancy
* MVCC engine
* Optimistic, non-blocking concurrency with N-writers and M-readers
* Pure Append-Only
* Unique data storage architecture
* Fast: O(1) worst for read, write and range scan operations
* Multi-threaded compaction
* Multi-databases support (sharing a single write-ahead log)
* Multi-Statement and Single-Statement Transactions (cross-database)
* Serialized Snapshot Isolation (SSI)
* Optimized storage schema (numeric types has zero-cost storage)
* Can be used to build Secondary Indexes
* Upsert (fast write-only 'update or insert' operation)
* Consistent Cursors
* Prefix search
* Automatic garbage-collection
* Automatic key-expire
* Hot Backup
* Compression (no fixed-size blocks, no-holes, supported: lz4, zstd)
* Direct IO support
* Use mmap or pread access methods
* Simple and easy to use (minimalistic API, FFI-friendly, amalgamated)
* Implemented as small *C-written* library with zero dependencies
* Carefully tested
* Open Source Software, BSD (*the storage engine*) and Apache v2.0 (*this driver*)

## Usage

In order to use the library add the dependency to your `project.clj`

``` clojure
[com.brunobonacci/clj-sophia "0.5.3"]
```

Current version: [![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/clj-sophia.svg)](https://clojars.org/com.brunobonacci/clj-sophia)

Supported platforms:

| Platform      | Supported       | Note                       |
|---------------|-----------------|----------------------------|
| linux-x86-64  | embedded        | linux 64-bits (not Alpine) |
| linux-x86     | need to install | linux 32-bits              |
| darwin-x86-64 | embedded        | Mac OSX 64-bits            |
| win32-x86     | need to install | Windows 32-bits            |
| win32-x86-64  | need to install | Windows 64-bits            |

For the platform you need to install please see [instructions
here](http://sophia.systems/v2.2/tutorial/build.html). Then add the
following Java property to your command line:
`-Djna.library.path=/path/to/sophia/lib`

  * Note: If you are running on Alpine linux you might experience
    crashes. In that case, please build the sophia as described above
    and add the shared library to your classpath or specify the
    `jna.library.path` property.

Require the namespace:

``` clojure
(ns foo.bar
  (:require [com.brunobonacci.sophia :as sph]))
```

### Create environment

Then create a sophia environment.

``` clojure
;; create a sophia environment.
(def env
  (sph/sophia
   {;; where to store the files on disk
    :sophia.path "/tmp/sophia-test"
    ;; which logical databases to create
    :dbs ["accounts", {:name "transactions"}]}))
```

### Basic operations

Now you can **get**/**set**/**delete** values with:

``` clojure
;; set a simple value
(sph/set-value!  env "accounts" "user1" "John")
;;=> "John"

;; get the value back
(sph/get-value   env "accounts" "user1")
;;=> "John"

;; delete a key
(sph/delete-key! env "accounts" "user1")
;;=> nil

;; now the key isn't present
(sph/get-value   env "accounts" "user1")
;;=> nil

;; set a complex value
(sph/set-value! env "accounts" "user1"
            {:firstname "John" :lastname "Doe" :age 34 :balance 100.0})
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}

;; get it back
(sph/get-value   env "accounts" "user1")
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}
```

Let's add more data:

``` clojure
(sph/set-value! env "accounts" "user2"
            {:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0})
;;=> :{:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0}

(sph/set-value! env "accounts" "admin1"
            {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]})
;;=> {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]}
```

### Range queries

You can run **range queries** and get a `lazy-seq` of ordered items
(by key).  This is very useful when working with timeseries but also
with other keys.  The function `range-query` takes a cursor which you
can create with the `cursor` function and a database name and return a
ordered lazy-sequence of entries where each entry is a tuple with the
key and the value, like: `[ key, value ]`.

Let's query the data:

``` clojure
;; display all the values in ascending order (by key)
(with-open [cur (sph/cursor env)]
  (run! prn
        (sph/range-query cur "accounts")))

;; ["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]
;; ["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;; ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]
```

`with-open` it closes the cursor at the end of the sexp, since `range-query`
returns a *lazy sequence* you need to ensure that you consume the sequence
withing the `with-open` form.

`range-query` returns a *lazy sequence* of tuples in the form of `[key value]`,
which conveniently can be turned into a Clojure hash-map with:

``` clojure
;; display all the values in ascending order (by key)
(with-open [cur (sph/cursor env)]
  (into {}
        (sph/range-query cur "accounts")))

;;=> {"admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]},
;;    "user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0},
;;    "user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}}

```

#### Control the key ordering (ascending/descending)

By default the range query returns the result in *ascending order*
(`:order :asc`) but you can reverse the order by passing `:order :desc`
to the `range-query` function.

``` clojure
;; display the content in descending oder
(with-open [cur (sph/cursor env)]
  (run! prn
        (sph/range-query cur "accounts" :order :desc)))

;; ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]
;; ["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;; ["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]
```

#### Seek and scan queries

The index can be considered as a ordered sequence of keys. One possibility
is to seek for a point in this sequence and walk the keys from that point
forward or backwards. This is possible with the `range-query` function.
This is very useful when querying timeseries keys.

``` clojure
;; let's find `user1` and walk the index forward
(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :asc)))

;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]]


;; let's find `user1` and walk the index backwards `:order :desc`
(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :desc)))

;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]]
```

You can also decide whether the key matched by the `key` argument must
be *included* or *excluded* from the search result. This is achieved
with the `:search-type :index-scan-exclusive` option, by default is:
`:search-type :index-scan-inclusive`

``` clojure
;; exclude the matching key
(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user1" :order :desc :search-type :index-scan-exclusive)))
;;=> [["admin1" {:firstname "Robert", :lastname "Green", :age 32, :grants [:accounts/admin]}]]
```

#### Prefix queries

If you want to retrieve all the items whose key start with a given prefix
you can provide the following options:

``` clojure
;; search for all the keys which starts with "user"
(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "user" :search-type :prefix)))

;;=> [["user1" {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}]
;;    ["user2" {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}]]


;; search for all the keys which starts with "foo"
(with-open [cur (sph/cursor env)]
  (into []
        (sph/range-query cur "accounts" :key "foo" :search-type :prefix)))
;;=> []
```

*Please note that the `:order` option is **not** available for `:prefix` queries
and won't affect the result.*

### Transactions

Updates to a single key are always atomic, however if you need to
change atomically multiple keys or need to change a single key in a
atomic `get -> update -> set` then you must use transactions.

For example let's say we want to credit John (`user1`) with $150.0.

``` clojure
;; initially the user1 has $100.0 as :balance
(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 100.0}

;; let's atomically credit $150.0 with a get -> update -> set statement
;; PLEASE NOTE that `get-value` and `set-value!` take a transaction
;; instead of a sophia environment.
(sph/with-transaction [tx (sph/begin-transaction env)]
  (let [user1 (sph/get-value tx "accounts" "user1")]
    (sph/set-value! tx "accounts" "user1" (update user1 :balance + 150.0))))
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 250.0}

;; now let's verify the the new balance
(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 250.0}
```

Finally let's see how we can transfer $200.0 from John to Jane as
example of multi-key transaction.

``` clojure
;; let's check Jane's initial balance
(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 200.0}

;; set up some parameters
(let [from   "user1"
      to     "user2"
      amount 200.0]
  ;; start transaction
  (sph/with-transaction [tx (sph/begin-transaction env)]
    (let [user1 (sph/get-value tx "accounts" from)
          user2 (sph/get-value tx "accounts" to)]
      ;; if funds are not available abort the transaction
      (when-not (>= (:balance user1) amount)
        (throw (ex-info (str "Insufficient funds in available from: " from)
                        {:from user1 :amount amount :to user2})))
      ;; these two update are inside a transaction and they will be stored
      ;; atomically
      (sph/set-value! tx "accounts" from (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" to   (update user2 :balance + amount )))))
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

;; now let's check the updated balances
(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 50.0}
```

Transactions can also span multiple databases as long as they
all belong to the same sophia environment

``` clojure
(require '[clj-time.core :as t]
         '[clj-time.format :as f])


(defn now-UTC
  "Returns the current UTC timestamp in ISO format"
  []
  (f/unparse (f/formatters :date-time) (t/now)))


(now-UTC)
;;=> "2018-03-21T22:15:52.512Z"

(let [from   "user2"
      to     "user1"
      amount 250.0]
  ;; start transaction
  (sph/with-transaction [tx (sph/begin-transaction env)]
    (let [user1 (sph/get-value tx "accounts" from)
          user2 (sph/get-value tx "accounts" to)]
      (when-not (>= (:balance user1) amount)
        (throw (ex-info (str "Insufficient funds in available from: " from)
                        {:from user1 :amount amount :to user2})))
      ;; add a entry in the `transactions`
      (sph/set-value! tx "transactions" (now-UTC)
                      {:tx-type :transfer :from from :amount amount :to to})
      ;; now update both user's balance
      (sph/set-value! tx "accounts" from (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" to   (update user2 :balance + amount )))))


(with-open [cur (sph/cursor env)]
  (into [] (sph/range-query cur "transactions")))
;;=> [["2018-03-21T22:26:16.284Z" {:tx-type :transfer, :from "user2", :amount 250.0, :to "user1"}]]

(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 150.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 300.0}
```

### `transact!`

Often you might want update one or more values in a transaction.  In a
highly concurrent system, transaction might fail because of the
concurrent modification. If you package your changes into a function
that reads all necessary data within the transaction and updates the
values still within the transaction boundaries than you can use
`transact!`. If the transaction fails `transact!` it will retry it
with a exponential back off.  The transaction will be retried until it
succeed similar to `clojure.core/swap!`.

For example, let assume that users have the ability to vote (or like)
other users, such function could be very concurrent for popular users.
Keeping track of the user's votes could be done using `transact!`

``` clojure
(sph/transact! env
  (fn [tx]
    (let [u (sph/get-value tx "accounts" "user1")]
      (when u
        (sph/set-value! tx "accounts" "user1"
                        ;; increment the number of votes
                        (update u :votes (fnil inc 0)))))))

;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 300.0, :votes 1}
```

If it fails because of concurrent updates, it will be automatically retried.

### `update-value!`

Since this is a common pattern to update the value of a single key
there is a simplified version called `update-value!`.
For example if we have a function that increases the age of a user
on a birthday:

``` clojure
(sph/update-value! env "accounts" "user1" (fn [u] (update u :age inc)))
;;=> {:firstname "John", :lastname "Doe", :age 35, :balance 300.0 :votes 1}

;; or more concisely

(sph/update-value! env "accounts" "user1" update :age inc)
;;=> {:firstname "John", :lastname "Doe", :age 36, :balance 300.0 :votes 1}
```

The `update-value!` function will lookup for the given key and apply
the function `f` if the key has been found. If the key doesn't exist
the function `f` is not called at all and `nil` is returned.

For example:

``` clojure
;; user10 doesn't exists.
(sph/update-value! env "accounts" "user10" update :age inc)
;;=> nil
```

### `upsert-value!`

There is a similar function called `upsert-value!` which behaves like
`update-value!` but the function `f` will be called even if the key
doesn't exists (with `nil`).

``` clojure
;; user10 doesn't exists.
(sph/upsert-value! env "accounts" "user10" update :votes (fnil inc 0))
;;=> {:votes 1}
```


## Configuration

Here the configuration options for sophia environment:

``` clojure
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
   }
```

Databases can be just strings or maps. Maps accepts the following
configuration options:

``` clojure
  {;; name of the database
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
   ;; :sync (s/both s/Int (s/enum 0 1))

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
   }
```

## Metrics

The following metrics are tracked via [TrackIt](https://github.com/samsara/trackit)

```
sophia.<db-name>.set-value.time
sophia.<db-name>.get-value.time
sophia.<db-name>.delete-key.time
sophia.<db-name>.serialization.time
sophia.<db-name>.deserialization.time
sophia.<db-name>.serialization.payload-size
sophia.<db-name>.deserialization.payload-size
sophia.<db-name>.range-query.seek.time
sophia.<db-name>.range-query.scan.time
```

Additionally all native engine metrics can be inspected and tracked as
well.  All metrics can be pushed to a number of different systems such
as: Console, Ganglia, Graphite, Statsd, Infuxdb, Reimann and NewRelic.

To enable reporting of these metrics please see [TrackIt
documentation](https://github.com/samsara/trackit#start-reporting).

## How to build.

To build locally this project proceed as following:

``` bash
# clone this repo
git clone https://github.com/BrunoBonacci/clj-sophia.git

# if you need to rebuild the native dependencies
cd clj-sophia/lib
# download and build sophia db
./build.sh
cd ..

# run the tests:
lein do clean, midje
```

## Version compatibility matrix

| From version | Until version | Sophia version | Note                                                             |
|:------------:|:-------------:|:--------------:|:-----------------------------------------------------------------|
| Up to        | 0.4.3         | v2.2 (eca1348) |                                                                  |
| 0.4.4        |               | v2.2 (669d57b) | [Important SSI fix](https://github.com/pmwkaa/sophia/issues/164) |


## License

clj-sophia (the driver) Copyright © 2018 Bruno Bonacci <br/>
Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

Sophia storage engine Copyright (C) Dmitry Simonenko (pmwkaa@gmail.com) <br/>
Distributed under BSD License (https://github.com/pmwkaa/sophia/blob/master/LICENSE)
