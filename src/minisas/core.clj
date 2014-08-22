(ns minisas.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [minisas.server :refer [new-webservice]]
            [minisas.values :refer [new-value-storage]]))


(defn read-config [path]
  (-> path io/resource slurp edn/read-string))


(defn system [config]
  (component/system-map
   :value-storage (new-value-storage config)
   :application (new-webservice config)))


(defn -main [& args]
  (let [config (read-config "config.edn")]
    (component/start (system config))))
