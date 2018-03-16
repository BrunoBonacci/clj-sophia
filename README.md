# clj-shopia

**Work in progress**

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
[com.brunobonacci/clj-sophia "0.1.0"]
```

Current version: [![safely](https://img.shields.io/clojars/v/com.brunobonacci/clj-sophia.svg)](https://clojars.org/com.brunobonacci/clj-sophia)


Require the namespace:

``` clojure
(ns foo.bar
  (:require [com.brunobonacci.sophia :as sph]))
```

### Create environment

Then create a sophia environment.

``` clojure

(defonce env (sph/sophia {:sophia.path "/tmp/sophia-test"
                          :db "accounts"}))

```

### Basic operations

Now you can **get**/**set**/**delete** values with:

``` clojure
;; set a simple value
(sph/set-value!  env "accounts" "user1" "John")
;;=> :ok

;; get the value back
(sph/get-value   env "accounts" "user1")
;;=> "John"

;; delete a key
(sph/delete-key! env "accounts" "user1")
;;=> :ok

;; now the key isn't present
(sph/get-value   env "accounts" "user1")
;;=> nil

;; set a complex value
(sph/set-value! env "accounts" "user1"
            {:firstname "John" :lastname "Doe" :age 34 :balance 100.0})
;;=> :ok

;; get it back
(sph/get-value   env "accounts" "user1")
;;=> {:firstname "John" :lastname "Doe" :age 34 :balance 100.0}
```

Let's add more data:

``` clojure
(sph/set-value! env "accounts" "user2"
            {:firstname "Jane" :lastname "Smith" :age 28 :balance 200.0})
;;=> :ok

(sph/set-value! env "accounts" "admin1"
            {:firstname "Robert" :lastname "Green" :age 32 :grants [:accounts/admin]})
;;=> :ok
```

### Range queries

You can run **range queries** and get a `lazy-seq` of ordered items (by key).
This is very useful when working with timeseries.

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
;;=> :ok

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
                        {:from user1 :amount 200.0 :to user2})))
      ;; these two update are inside a transaction and they will be stored
      ;; atomically
      (sph/set-value! tx "accounts" "user1" (update user1 :balance - amount ))
      (sph/set-value! tx "accounts" "user2" (update user2 :balance + amount )))))
;;=> :ok

;; now let's check the updated balances
(sph/get-value env "accounts" "user2")
;;=> {:firstname "Jane", :lastname "Smith", :age 28, :balance 400.0}

(sph/get-value env "accounts" "user1")
;;=> {:firstname "John", :lastname "Doe", :age 34, :balance 50.0}
```

## How to build.

To build locally this project proceed as following:

``` bash
# clone this repo
git clone https://github.com/BrunoBonacci/clj-sophia.git
cd clj-sophia/lib
# download and build sophia db
./build.sh
cd ..

# run the tests:
lein do clean, midje
```

## License

clj-sophia (the driver) Copyright © 2018 Bruno Bonacci <br/>
Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

Sophia storage engine Copyright (C) Dmitry Simonenko (pmwkaa@gmail.com) <br/>
Distributed under BSD License (https://github.com/pmwkaa/sophia/blob/master/LICENSE)
