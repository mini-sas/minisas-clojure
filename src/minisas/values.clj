(ns minisas.values
  (:require [com.stuartsierra.component :as component]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [org.jclouds.blobstore2 :refer [blobstore
                                            create-container
                                            put-blob
                                            get-blob
                                            blob
                                            blob-exists?]]
            [ring.util.response :as response]
            [ring.util.time :refer [format-date]]))


(defrecord ValueStorage [config]
  component/Lifecycle

  (start [this]
    (let [store (blobstore (-> this :config :jclouds :provider)
                           (-> this :config :jclouds :username)
                           (-> this :config :jclouds :password))
          container-name (-> this :config :jclouds :container)]
      (create-container store container-name)
      (assoc this
        :blobstore store
        :container container-name)))

  (stop [this] this))


(defn new-value-storage [config]
  (map->ValueStorage {:config config}))


(defn value-storage [ctx] (get-in ctx [:request :value-storage]))
(defn blob-store [ctx] (-> ctx value-storage :blobstore))
(defn container-name [ctx] (-> ctx value-storage :container))
(defn blob-name [uuid] (str uuid))


(defn post! [ctx]
  (let [uuid (java.util.UUID/randomUUID)
        value (get-in ctx [:request :body])
        b (blob (blob-name uuid)
                :payload value
                :content-type (get-in ctx [:request :headers "content-type"]))]
    (put-blob (blob-store ctx) (container-name ctx) b)
    {:uuid uuid}))


(defn exists? [ctx name]
  (try
    (let [uuid (java.util.UUID/fromString name)
          exists (blob-exists? (blob-store ctx) (container-name ctx) (blob-name uuid))]
      [exists {:uuid uuid :blob-exists exists}])
    (catch IllegalArgumentException e false)))


(defn one-year-from [when]
  (-> (doto (java.util.Calendar/getInstance)
        (.setTime when)
        (.add java.util.Calendar/YEAR 1))
      (.getTime)))


;; NOTE: content-type doesn't seem to be saved in filesystem storage.
(defn get-value [ctx name]
  (let [b (get-blob (blob-store ctx)
                    (container-name ctx)
                    (-> ctx :uuid blob-name))
        ctype (-> b (.getMetadata) (.getContentMetadata) (.getContentType))]

    (-> (response/response (-> b (.getPayload) (.openStream)))
        (response/header "Expires" (-> (java.util.Date.)
                                       one-year-from
                                       format-date))
        (response/header "Immutable" true)
        (response/header "Content-Type" ctype)
        ring-response)))


(defn build-entry-url [request id]
  (java.net.URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))


(defresource named-value [name]
  :available-media-types ["*/*"]
  :allowed-methods [:get]
  :exists? (fn [ctx] (exists? ctx name))
  :handle-ok (fn [ctx] (get-value ctx name))
  :handle-exception (fn [ctx] (throw (:exception ctx))))


(defresource named-value-set
  :available-media-types ["*/*"]
  :allowed-methods [:post]
  :post! post!
  :post-redirect? true
  :location (fn [ctx] (build-entry-url (:request ctx) (:uuid ctx))))