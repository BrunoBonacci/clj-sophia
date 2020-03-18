(defproject com.brunobonacci/clj-sophia "0.5.4-SNAPSHOT"

  :description "An idiomatic Clojure driver for Sophia DB. Sophia is
                RAM-Disk hybrid storage designed to provide best
                possible on-disk performance without degradation in
                time."

  :url "https://github.com/BrunoBonacci/clj-sophia"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/clj-sophia"}

  :dependencies [[net.java.dev.jna/jna "5.5.0"]
                 [com.taoensso/nippy "2.14.0"]
                 [prismatic/schema "1.1.12"]
                 [samsara/trackit-core "0.9.3"]
                 [com.brunobonacci/safely "0.5.0"]]

  :global-vars {*warn-on-reflection* true}

  :profiles
  {:1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}

   :dev [:1.9
         {:dependencies [[midje "1.9.9"]
                         [org.clojure/test.check "1.0.0"]
                         [criterium "0.4.5"]]
          :plugins [[lein-midje "3.2.2"]]}]}

  :aliases
  {"test-all"   ["do" "clean,"
                 "with-profile" "+1.10:+1.9:+1.8" "midje"]}

  :jvm-opts ["-server"
             #_"-Djna.library.path=./lib/sophia"
             #_"-Djna.debug_load=true"])
