(ns com.brunobonacci.sophia.native
  (:import [com.sun.jna Library Native Platform Pointer]
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
    [sp_setstring [com.sun.jna.Pointer String String int] int]

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

(defn sp-env []
  (.sp_env jns))

(defn sp-setstring [env key value]
  (.sp_setstring jns env key value 0))

(defn sp-getbytes [doc key]
  (let [size (IntByReference.)
        value* (.sp_getstring jns doc key size)]
    (when value*
      (.getByteArray value* (long 0) (.getValue size)))))

(defn sp-getstring [doc key
                    & {:keys [encoding]
                       :or {encoding (System/getProperty "file.encoding" "utf8")}}]
  (when-let [bytez (sp-getbytes doc key)]
    (String. ^bytes bytez ^String encoding)))

(defn sp-open [env]
  (.sp_open jns env))

(defn sp-document [db]
  (.sp_document jns db))

(defn sp-set [db doc]
  (.sp_set jns db doc))

(defn sp-get [db doc]
  (.sp_get jns db doc))

(defn sp-getobject [env key]
  (.sp_getobject jns env key))

(defn sp-destroy [ref]
  (.sp_destroy jns ref))

(defn sp-error [env]
  (sp-getstring env "sophia.error"))


(comment

    ;; void* env = sp_env();
  (def env (sp-env))


  ;; sp_setstring(env, "sophia.path", "./storage", 0);
  (sp-setstring env "sophia.path" "/tmp/sophia.db")

  ;; sp_setstring(env, "db", "test", 0);
  (sp-setstring env "db" "test")

  ;; sp_open(env);
  (sp-open env)


  ;; void* db = sp_getobject(env, "db.test");
  (def db (sp-getobject env "db.test"))

  ;; /* do transactions */

  ;; SET
  ;; void* o = sp_document(db);
  (def o (sp-document db))

  ;; sp_setstring(o, "key", &key, sizeof(key));
  ;; sp_setstring(o, "value", &key, sizeof(key));

  (sp-setstring o "key" "firstname")
  (sp-setstring o "value" "Bruno")

  ;; rc = sp_set(db, o);
  (sp-set db o)

  ;;
  ;; GET
  ;;

  ;; void* o = sp_document(db);
  (def o (sp-document db))

  (sp-setstring o "key" "firstname")

  (def o (sp-get db o))

  (when-not o
    (println "not found"))

  (sp-getstring o "key")
  (sp-getstring o "value")
  (sp-getstring o "dob")

  ;; sp_destroy(o);
  (sp-destroy o)


  ;; sp_destroy(env);
  (.sp_destroy jns env)



  (sp-error env)

  )


(comment

    ;; void* env = sp_env();
  (def env (.sp_env jns))


  ;; sp_setstring(env, "sophia.path", "./storage", 0);
  (.sp_setstring jns env "sophia.path" "/tmp/sophia.db" 0)

  ;; sp_setstring(env, "db", "test", 0);
  (.sp_setstring jns env "db" "test" 0)

  ;; sp_open(env);
  (.sp_open jns env)


  ;; void* db = sp_getobject(env, "db.test");
  (def db (.sp_getobject jns env "db.test"))

  ;; /* do transactions */

  ;; SET
  ;; void* o = sp_document(db);
  (def o (.sp_document jns db))

  ;; sp_setstring(o, "key", &key, sizeof(key));
  ;; sp_setstring(o, "value", &key, sizeof(key));

  (.sp_setstring jns o "key" "hello" 0)
  (.sp_setstring jns o "value" "World!2" 0)

  ;; rc = sp_set(db, o);
  (.sp_set jns db o)

  ;;
  ;; GET
  ;;

  ;; void* o = sp_document(db);
  (def o (.sp_document jns db))

  (.sp_setstring jns o "key" "hello1" 0)

  (def o (.sp_get jns db o))

  (when-not o
    (println "not found"))

  (def size (IntByReference.))

  (def v* (.sp_getstring jns o "valued" size))

  (.getValue size)

  (def v (.getByteArray v* (long 0) (.getValue size)))

  (def k (String. v "utf8"))

  (println k)


  ;; sp_destroy(o);
  (.sp_destroy jns o)


  ;; sp_destroy(env);
  (.sp_destroy jns env)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;                                                                            ;;
  ;;                         ----==| E R O R R |==----                          ;;
  ;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (.sp_setstring jns o "foo" "hello" 0)

  (def v* (.sp_getstring jns env "sophia.error" size))



  )
