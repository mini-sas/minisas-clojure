(ns minisas.server
  (:require [compojure.core :refer [ANY defroutes]]
            [com.stuartsierra.component :as component]
            [liberator.dev :refer [wrap-trace]]
            [minisas.references :refer [reference]]
            [minisas.values :refer [named-value named-value-set]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]))


(defroutes minisas-service
  (ANY "/value" [] named-value-set)
  (ANY "/value/:name" [name] (named-value name))
  (ANY "/reference/*" {{name :*} :params} (reference name)))


(defn wrap-request [f k c]
  (fn [req] (f (assoc req k c))))


(defrecord WebService [config value-storage]
  component/Lifecycle

  (start [this]
    (let [app (-> minisas-service
                  wrap-reload
                  wrap-params
                  (wrap-request :value-storage (:value-storage this)))
          jetty (run-jetty app {:port (-> this :config :server :port)
                                :join? false})]
      (assoc this :jetty jetty)))

  (stop [this]
    (.stop (:jetty this))
    (assoc this :jetty nil)))


(defn new-webservice [config]
  (component/using
   (map->WebService {:config config})
   [:value-storage]))
