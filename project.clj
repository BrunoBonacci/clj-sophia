(defproject com.brunobonaci/clj-shopia "0.1.0-SNAPSHOT"
  :description "a Clojure driver for sophia DB."
  :url "https://github.com/BrunoBonacci/clj-sophia"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/clj-sohpia"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.java.dev.jna/jna "4.5.1"]]

  :jvm-opts ["-Djna.library.path=./lib/sophia"])
