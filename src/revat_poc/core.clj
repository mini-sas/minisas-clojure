(ns revat-poc.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [revat-poc.server :refer [new-webservice]]
            [revat-poc.values :refer [new-value-storage]]))



(defn system [config]
  (component/system-map
   :value-storage (new-value-storage config)
   :application (new-webservice config)))

(defn -main [& args]
  (println "starting")
  (let [config {:port 8080}]
    (component/start (system config))))
