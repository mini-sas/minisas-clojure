(defproject revat-poc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.jclouds/jclouds-all "1.8.0"]
                 [ring "1.3.0"]
                 [liberator "0.12.1"]
                 [compojure "1.1.8"]
                 [org.clojure/tools.logging "0.3.0"]
                 [com.stuartsierra/component "0.2.1"]]
  :main ^:skip-aot revat-poc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
