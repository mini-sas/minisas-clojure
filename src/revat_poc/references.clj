(ns revat-poc.references
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [ring.util.response :as response]))

;; TODO: some kind of persistence for the POC
(def references (atom {}))


(defn exists? [ctx name]
  (println name)
  (let [refs-snapshot @references]
    [(contains? refs-snapshot name)
     {:refs-snapshot refs-snapshot}]))


(defn handle-deref [ctx name]
  (let [url (get-in ctx [:refs-snapshot name])]
    (-> url str response/redirect ring-response)))


(defn valid-url? [url-str]
  (try
    (java.net.URL. url-str)
    (catch java.net.MalformedURLException e false)))


(defn known-content-type? [ctx]
  ;; parse wonky content-type pieces
  ;; validate that shit
  ;; attach to the ctx
  (let [ctype (get-in ctx [:request :headers "content-type"])]
    (println ctype)
    true
    )

  )

(defn malformed? [ctx]
  (when (= :put (-> ctx :request :request-method))
    (let [body (-> ctx :request :body slurp)
          url (valid-url? body)]
      [(not url) {:url url}])))


(defn put! [ctx name]
  (swap! references assoc name (:url ctx)))


(defresource reference [name]
  :available-media-types ["*/*"]
  :allowed-methods [:put :get]
  :can-put-to-missing? true
  :known-content-type? known-content-type?
  :malformed? malformed?
  :exists? (fn [ctx] (exists? ctx name))
  :handle-ok (fn [ctx] (handle-deref ctx name))
  :handle-exception (fn [ctx] (println (:exception ctx)))
  :put! (fn [ctx] (put! ctx name)))


;; http://tools.ietf.org/html/rfc1521#page-42
;; message/external-body
;; Content-type: message/external-body; access-type=URL;
;;                  URL="http://www.foo.com/file"