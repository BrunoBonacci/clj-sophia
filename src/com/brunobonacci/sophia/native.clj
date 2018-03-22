(ns com.brunobonacci.sophia.native
  (:import [com.sun.jna Library Native Platform Pointer Memory]
           [com.sun.jna.ptr IntByReference]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| N A T I V E   I N T E R F A C E |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(gen-interface
 :name com.brunobonacci.sophia.jna.Sophia
 :extends [com.sun.jna.Library]
 :methods
 [;; void* sp_env(void);
  [sp_env       [] com.sun.jna.Pointer]

  ;; void* sp_document(void* database);
  ;;[sp_document  [com.sun.jna.Pointer] com.sun.jna.Pointer]
  [sp_document  [com.sun.jna.Pointer] com.sun.jna.Pointer]

  ;; int sp_setstring(void* object, const char *path, const void* ptr, int size);
  ;;[sp_setstring [com.sun.jna.Pointer String String int] int]
  ;;[sp_setstring [com.sun.jna.Pointer String bytes int] int]
  [sp_setstring [com.sun.jna.Pointer String com.sun.jna.Pointer int] int]

  ;; int sp_setint(void* object, const char *path, int64_t value);
  [sp_setint    [com.sun.jna.Pointer String long] int]

  ;; void* sp_getobject(void* object, const char *path);
  [sp_getobject [com.sun.jna.Pointer String] com.sun.jna.Pointer]

  ;; void* sp_getstring(void* object, const char *path, int *size);
  [sp_getstring [com.sun.jna.Pointer String com.sun.jna.ptr.IntByReference] com.sun.jna.Pointer]

  ;; int64_t sp_getint(void* object, const char *path);
  [sp_getint    [com.sun.jna.Pointer String] long]

  ;; int sp_open(void* env);
  ;; int sp_open(void* database);
  [sp_open      [com.sun.jna.Pointer] int]

  ;; int sp_destroy(void* object);
  [sp_destroy   [com.sun.jna.Pointer] int]

  ;; int sp_set(void* database, void* document);
  ;; int sp_set(void* transaction, void* document);
  [sp_set       [com.sun.jna.Pointer com.sun.jna.Pointer] int]

  ;; void* sp_upsert(void* database, void* document);
  ;; void* sp_upsert(void* transaction, void* document);
  [sp_upsert    [com.sun.jna.Pointer com.sun.jna.Pointer] com.sun.jna.Pointer]

  ;; int sp_delete(void* database, void* document);
  ;; int sp_delete(void* transaction, void* document);
  [sp_delete    [com.sun.jna.Pointer com.sun.jna.Pointer] int]

  ;; void* sp_get(void* database, void* document);
  ;; void* sp_get(void* transaction, void* document);
  [sp_get       [com.sun.jna.Pointer com.sun.jna.Pointer] com.sun.jna.Pointer]

  ;; void* sp_cursor(void* env);
  [sp_cursor    [com.sun.jna.Pointer] com.sun.jna.Pointer]

  ;; void* sp_begin(void* env);
  [sp_begin     [com.sun.jna.Pointer] com.sun.jna.Pointer]

  ;; int sp_commit(void* transaction);
  [sp_commit    [com.sun.jna.Pointer] int]
  ])



(def ^com.brunobonacci.sophia.jna.Sophia jns
  (com.sun.jna.Native/loadLibrary
   "sophia" com.brunobonacci.sophia.jna.Sophia))



(defn- default-encoding
  []
  (System/getProperty "file.encoding" "utf8"))



(defn bytes->void* [^bytes value]
  (let [p (Memory. (count value))]
    (.write p 0 value 0 (count value))
    p))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| N A T I V E   A P I S |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn sp_env
  "sp_env - create a new environment handle

  DESCRIPTION:

  The sp_env() function allocates new Sophia environment object.

  The object is intended for usage by sp_open() and must be configured
  first. After using, an object should be freed by sp_destroy().

  Please take a look at Configuration, and Database administration
  sections.

  RETURN VALUE:

  On success, sp_env() allocates new environment object pointer. On
  error, it returns NULL.
  "
  []
  (.sp_env jns))



(defn sp_document
  "sp_document - create a document object

  DESCRIPTION:

  sp_document(database): create new document for a transaction on a
  selected database.

  The sp_document() function returns an object which is intended to be
  used in by any CRUD operations. Document might contain a key-value
  pair with any additional metadata.

  RETURN VALUE:

  On success, sp_document() returns an object pointer. On error, it
  returns NULL.

  "
  [db]
  (.sp_document jns db))



(defn sp_setbytes
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [env key ^bytes value]
  ;;(.sp_setstring jns env key value (count value))
  (.sp_setstring jns env key (bytes->void* value) (count value)))



(defn sp_setstring
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [env key ^String value
   & {:keys [encoding]
      :or {encoding (default-encoding)}}]
  (sp_setbytes env key (.getBytes value encoding)))



(defn sp_setint
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [env key ^long value]
  (.sp_setint jns env key value))



(defn sp_getobject
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [env key]
  (.sp_getobject jns env key))



(defn sp_getbytes
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [doc key]
  (let [size (IntByReference.)
        value* (.sp_getstring jns doc key size)]
    (when value*
      (.getByteArray value* (long 0) (.getValue size)))))



(defn sp_getstring
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [doc key
   & {:keys [encoding]
      :or {encoding (default-encoding)}}]
  (when-let [bytez (sp_getbytes doc key)]
    (String. ^bytes bytez ^String encoding)))



(defn sp_getint
  "sp_setstring, sp_getstring, sp_setint, sp_getint, sp_getobject - set
  or get configuration options

  DESCRIPTION:

  For additional information take a look at the Configuration section.

  RETURN VALUE:

  On success, sp_setstring() returns 0. On error, it returns -1.

  On success, sp_getstring() returns string pointer. On error or if
  the variable is not set, it returns NULL.

  All pointers returned by sp_getstring() must be freed using free(3)
  function. Exception is sp_document() object and configuration cursor
  document.

  On success, sp_setint() returns 0. On error, it returns -1. On
  success, sp_getint() returns a numeric value. On error, it returns
  -1.

  On success, sp_getobject() returns an object pointer. On error or if
  the variable is not set, it returns NULL.

  The database object returned by sp_getobject() increments its
  reference counter, sp_destroy() can be used to decrement it. This
  should be considered for online database close/drop cases.
  "
  [doc key]
  (.sp_getint jns doc key))



(defn sp_open
  "sp_open - open or create

  DESCRIPTION:

  sp_open(env): create environment, open or create pre-defined
  databases.

  sp_open(database): create or open database.

  Please take a look at Configuration, and Database administration
  sections.

  RETURN VALUE:

  On success, sp_open() returns 0. On error, it returns -1.
  "
  [env]
  (.sp_open jns env))



(defn sp_destroy
  "sp_destroy - free or destroy an object

  DESCRIPTION:

  The sp_destroy() function is used to free memory allocated by any
  Sophia object.

  RETURN VALUE:

  On success, sp_destroy() returns 0. On error, it returns -1.
  "
  [ref]
  (.sp_destroy jns ref))



(defn sp_set
  "sp_set - insert or replace operation

  DESCRIPTION:

  sp_set(database, document): do a single-statement transaction.

  sp_set(transaction, document): do a key update as a part of
  multi-statement transaction.

  As a part of a transactional statement a key-value document must be
  prepared using sp_document() method. First argument of sp_document()
  method must be a database object.

  Object must be prepared by setting key and value fields, where value
  is optional. It is important that while setting key and value
  fields, only pointers are copied. Real data copies only during first
  operation.

  For additional information take a look at sp_document(), sp_begin()
  and Transactions.

  RETURN VALUE:

  On success, sp_set() returns 0. On error, it returns -1.

  Database object commit: (1) rollback or (2) lock.
  "
  [db doc]
  (.sp_set jns db doc))



(defn sp_upsert
  "sp_upsert - common get operation

  DESCRIPTION:

  sp_upsert(database, document): do a single-statement transaction.

  sp_upsert(transaction, document): do a key update as a part of
  multi-statement transaction.

  As a part of a transactional statement a key-value document must be
  prepared using sp_document() method. First argument of sp_document()
  method must be a database object.

  Object must be prepared by setting key and value fields. It is
  important that while setting key and value fields, only pointers are
  copied. Real data copies only during first operation.

  Value field should contain user-supplied data, which should be
  enough to implement custom update or insert logic.

  To enable upsert command, a db.database_name.index.upsert and
  optionally db.database_name.index.upsert_arg must be set to callback
  function pointer.

  For additional information take a look at sp_document(), sp_begin()
  and Transactions and Upsert sections.

  RETURN VALUE:

  On success, sp_set() returns 0. On error, it returns -1.

  Database object commit: (1) rollback or (2) lock.
  "
  [db doc]
  (.sp_upsert jns db doc))



(defn sp_delete
  "sp_delete - delete operation

  DESCRIPTION:

  sp_delete(database, document): do a single-statement transaction.

  sp_delete(transaction, document): do a key deletion as a part of
  multi-statement transaction.

  As a part of a transactional statement a key-value document must be
  prepared using sp_document() method. First argument of sp_document()
  method must be a database object.

  Object must be prepared by setting key fields. Value is not used for
  delete operation. It is important that while setting key fields,
  only pointers are copied. Real data copies only during first
  operation.

  For additional information take a look at sp_document(), sp_begin()
  and Transactions.

  RETURN VALUE:

  On success, sp_delete() returns 0. On error, it returns -1.

  Database object commit: (1) rollback or (2) lock.
  "
  [db doc]
  (.sp_delete jns db doc))



(defn sp_get
  "sp_get - common get operation

  DESCRIPTION

  sp_get(database, document): do a single-statement transaction read.

  sp_get(transaction, document): do a key search as a part of
  multi-statement transaction visibility.

  sp_get() method returns a document that is semantically equal to
  sp_document(), but is read-only.

  For additional information take a look at sp_begin() and
  Transactions.

  RETURN VALUE:

  On success, sp_get() returns a document handle. If an object is not
  found, returns NULL. On error, it returns NULL.
  "
  [db doc]
  (.sp_get jns db doc))



(defn sp_cursor
  "sp_cursor - common cursor operation

  DESCRIPTION:

  sp_cursor(env): create a cursor ready to be used with any database.

  For additional information take a look at Cursor section.

  RETURN VALUE:

  On success, sp_cursor() returns cursor object handle. On error, it
  returns NULL.
  "
  [env]
  (.sp_cursor jns env))



(defn sp_begin
  "sp_begin - start a multi-statement transaction

  DESCRIPTION:

  sp_begin(env): create a transaction

  During transaction, all updates are not written to the database
  files until a sp_commit() is called. All updates that were made
  during transaction are available through sp_get() or by using
  cursor.

  The sp_destroy() function is used to discard changes of a
  multi-statement transaction. All modifications that were made during
  the transaction are not written to the log file.

  No nested transactions are supported.

  For additional information take a look at Transactions and Deadlock
  sections.

  RETURN VALUE:

  On success, sp_begin() returns transaction object handle. On error,
  it returns NULL.
  "
  [env]
  (.sp_begin jns env))



(defn sp_commit
  "sp_commit - commit a multi-statement transaction

  DESCRIPTION:

  sp_commit(transaction): commit a transaction

  The sp_commit() function is used to apply changes of a
  multi-statement transaction. All modifications that were made during
  the transaction are written to the log file in a single batch.

  If commit failed, transaction modifications are discarded.

  For additional information take a look at Transactions and Deadlock
  sections.

  RETURN VALUE:

  On success, sp_commit() returns 0. On error, it returns -1. On
  rollback 1 is returned, 2 on lock.
  "
  [tx]
  (.sp_commit jns tx))


(defn c-string->jvm
  [^String s]
  (when s
    (let [bytez (.getBytes s)]
      (String. bytez 0 (Math/max 0 (dec (count bytez)))))))



(defn sp_lasterror [env]
  (c-string->jvm
   (sp_getstring env "sophia.error")))



(defmacro op [env & body]
  `(let [r# (do ~@body)]
     (if (= r# -1)
       (throw (ex-info (str "Database error: " (sp_lasterror ~env)) {}))
       r#)))



(defmacro with-ref
  "When `v` is not `nil` then body is executed and `v`
  is destroyed (freed) after."
  [v & body]
  `(let [v# ~v]
     (when v#
       (try
         ~@body
         (finally
           (sp_destroy v#))))))


(comment
  ;;
  ;; CRUD example from the website
  ;;

  ;; void* env = sp_env();
  (def env (sp_env))


  ;; sp_setstring(env, "sophia.path", "./storage", 0);
  (sp_setstring env "sophia.path" "/tmp/sophia.db")

  ;; sp_setstring(env, "db", "test", 0);
  (sp_setstring env "db" "test")

  ;; sp_open(env);
  (sp_open env)


  ;; void* db = sp_getobject(env, "db.test");
  (def db (sp_getobject env "db.test"))

  ;; /* do transactions */

  ;; SET
  ;; void* o = sp_document(db);
  (def o (sp_document db))

  ;; sp_setstring(o, "key", &key, sizeof(key));
  ;; sp_setstring(o, "value", &key, sizeof(key));

  (sp_setstring o "key" "firstname")
  (sp_setstring o "value" "Joe")

  ;; or

  (sp_setbytes o "key" (byte-array [102  105  114  115  116  110  97  109  101]))
  (sp_setbytes o "value" (byte-array [66  114  117  110  111]))

  ;; rc = sp_set(db, o);
  (sp_set db o)

  ;;
  ;; GET
  ;;

  ;; void* o = sp_document(db);
  (def o (sp_document db))

  (sp_setstring o "key"  "firstname")

  (def o (sp_get db o))

  (when-not o
    (println "not found"))

  (sp_getstring o "key")
  (sp_getstring o "value")

  ;; sp_destroy(o);
  (sp_destroy o)


  (sp_lasterror env)

  )
