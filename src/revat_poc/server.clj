(ns revat-poc.server
  (:require [compojure.core :refer [ANY defroutes]]
            [com.stuartsierra.component :as component]
            [liberator.dev :refer [wrap-trace]]
            [revat-poc.values :refer [named-value]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]))


(defroutes revat-service
  (ANY "/value/:name" [name] (named-value name)))


(defn wrap-request [f k c]
  (fn [req] (f (assoc req k c))))


(def app (-> revat-service
             wrap-params
             wrap-stacktrace))


(defrecord WebService [config value-storage]
  component/Lifecycle

  (start [this]
    (let [app (-> revat-service
                  wrap-reload
                  wrap-trace
                  wrap-stacktrace
                  wrap-params
                  (wrap-request :value-storage (:value-storage this)))
          jetty (run-jetty app {:port (get-in this [:config :port])
                                :join? false})]
      (assoc this :jetty jetty)))

  (stop [this]
    (.stop (:jetty this))
    (assoc this :jetty nil)))


(defn new-webservice [config]
  (component/using
   (map->WebService {:config config})
   [:value-storage]))
