(defproject com.brunobonacci/clj-shopia "0.1.0"
  :description "a Clojure driver for sophia DB."
  :url "https://github.com/BrunoBonacci/clj-sophia"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/clj-sohpia"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.java.dev.jna/jna "4.5.1"]
                 [com.taoensso/nippy "2.14.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.1"]
                                  [org.clojure/test.check "0.9.0"]
                                  [criterium "0.4.4"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  :jvm-opts ["-server"
             #_"-Djna.library.path=./lib/sophia"
             #_"-Djna.debug_load=true"])
