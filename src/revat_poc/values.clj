(ns revat-poc.values
  (:require [com.stuartsierra.component :as component]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [org.jclouds.blobstore2 :refer [blobstore
                                            create-container
                                            put-blob
                                            get-blob
                                            blob
                                            blob-exists?]]
            [ring.util.time :refer [format-date]]))


;; TODO: configgy stuff in the config
(defrecord ValueStorage [config]
  component/Lifecycle

  (start [this]
    (let [store (blobstore "filesystem" "foo" "bar" :jclouds.filesystem.basedir "/tmp")
          container (create-container store "revat-values")]
      (assoc this
        :blobstore store
        :container "revat-values"
        :in-flight (atom #{}))))

  (stop [this] this))


(defn new-value-storage [config]
  (map->ValueStorage {:config config}))


(defn value-storage [ctx] (get-in ctx [:request :value-storage]))
(defn blob-store [ctx] (-> ctx value-storage :blobstore))
(defn container-name [ctx] (-> ctx value-storage :container))
(defn blob-name [uuid] (str uuid))
(defn in-flight? [uuid ctx] (contains? (-> ctx value-storage :in-flight deref) uuid))


;; TODO: atomic exists? and put so we don't overwrite.
(defn put-value [ctx uuid]
  (let [value (get-in ctx [:request :body])
        b (blob (blob-name uuid)
                :payload value
                :content-type (get-in ctx [:request :headers "content-type"]))]
    (put-blob (blob-store ctx) (container-name ctx) b)))


(defn exists? [ctx name]
  (try
    (let [uuid (java.util.UUID/fromString name)
          exists (blob-exists? (blob-store ctx) (container-name ctx) (blob-name uuid))]
      [exists {:uuid uuid :blob-exists exists}])
    (catch IllegalArgumentException e false)))


(defn year-from-now []
  (-> (doto (java.util.Calendar/getInstance)
        (.setTime (java.util.Date.))
        (.add java.util.Calendar/YEAR 1))
      (.getTime)))


;; TODO: content-type doesn't seem to be saved in filesystem storage.
(defn get-value [ctx name]
  (let [b (get-blob (blob-store ctx)
                    (container-name ctx)
                    (-> ctx :uuid blob-name))
        ctype (-> b (.getMetadata) (.getContentMetadata) (.getContentType))]
    (ring-response {:status 200
                    :headers {"Immutable" ""
                              "Expires" (format-date (year-from-now))
                              "Content-Type" ctype}
                    :body (-> b (.getPayload) (.openStream))})))


(defn conflict? [ctx]
  (when (= :put (get-in ctx [:request :request-method]))
    (:blob-exists ctx)))


;; TODO a post! endpoint
(defresource named-value [name]
  :available-media-types ["*/*"]
  :allowed-methods [:get :put :post]
  :exists? (fn [ctx] (exists? ctx name))
  :conflict? conflict?
  :handle-ok (fn [ctx] (get-value ctx name))
  :handle-exception (fn [ctx] (throw (:exception ctx)))
  :put! (fn [ctx] (put-value ctx (:uuid ctx))))
