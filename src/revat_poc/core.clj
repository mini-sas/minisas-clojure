(ns revat-poc.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [revat-poc.server :refer [new-webservice]]
            [revat-poc.values :refer [new-value-storage]]))


(defn read-config [path]
  (-> path slurp edn/read-string))


(defn system [config]
  (component/system-map
   :value-storage (new-value-storage config)
   :application (new-webservice config)))


(defn -main [& args]
  (let [config (read-config "resources/config.edn")]
    (component/start (system config))))
