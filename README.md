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

Bindings for the most common languages are available
[here](http://sophia.systems/drivers.html).

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
* Open Source Software, BSD

## Usage

TODO:

## License

clj-sophia (the driver) Copyright © 2018 Bruno Bonacci <br/>
Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

Sophia storage engine Copyright (C) Dmitry Simonenko (pmwkaa@gmail.com) <br/>
Distributed under BSD License (https://github.com/pmwkaa/sophia/blob/master/LICENSE)
