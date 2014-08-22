(ns minisas.references
  (:require [clojure.string :as string]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [ring.util.response :as response]))

;; TODO: some kind of persistence for the POC
(def references (atom {}))


(defn exists? [ctx name]
  (let [refs-snapshot @references]
    [(contains? refs-snapshot name)
     {:refs-snapshot refs-snapshot}]))


(defn handle-deref [ctx name]
  (let [url (get-in ctx [:refs-snapshot name])]
    (-> url str response/redirect ring-response)))


(defn valid-url? [url-str]
  (try
    (java.net.URL. url-str)
    (catch java.net.MalformedURLException e false)
    (catch NullPointerException e false)))


(defn put-request? [ctx]
  (= :put (-> ctx :request :request-method)))


(defn trimsplit [s delimiter]
  (map string/trim (.split s delimiter)))


(defn parse-content-type [ctx]
  ;; Content-Type looks like this:
  ;;   message/external-body; access-type=URL; URL="http://www.foo.com/file"

  (let [entry (get-in ctx [:request :headers "content-type"])
        [content-type & param-list] (trimsplit entry ";")
        param-map (reduce merge {}
                          (map (fn [p] (let [[k v] (trimsplit p "=")]
                                         {(-> k (.toLowerCase) keyword) v}))
                               param-list))]
    {:content-type (.toLowerCase content-type)
     :params param-map}))


(defn cas-header-ok? [ctx]
  (let [cas-header (get-in ctx [:request :headers "if-match"])
        cas-url (valid-url? cas-header)]
    (if (nil? cas-header)
      [true {}]
      [cas-url {:cas-url cas-url}])))


(defn processable? [ctx]
  (if (put-request? ctx)
    (let [url (valid-url? (:url-string ctx))
          cas-result (cas-header-ok? ctx)] ;; could be composed better :-[
      [(and url (first cas-result)) (merge {:url url} (second cas-result))])
    true))


(defn known-content-type? [ctx]
  (if (put-request? ctx)
    (let [{:keys [content-type params]} (parse-content-type ctx)]
      [(= "message/external-body" content-type)
       {:url-string (:url params)}])
    true))


(defn compare-and-set-swapfn [name oldval newval]
  ;; I'm going to hell for this.
  (fn [a]
    (let [current-val (get a name)]
      (when (not (= current-val oldval))
        (throw (ex-info "CAS Failed" {:cas-failed true
                                      :expected oldval
                                      :was current-val})))
      (assoc a name newval))))


(defn handle-exception [ctx]
  ;; Liberator shortcoming. Can't bail out from put! with a failure/status code.
  (let [details (-> ctx :exception ex-data)]
    (if (seq details)
      (ring-response (-> (response/response "Compare-And-Set failed")
                         (response/status 412)))
      (.printStackTrace (:exception ctx)))))


(defn put! [ctx name]
  (let [cas-oldval (valid-url? (get-in ctx [:request :headers "if-match"]))]
    (if cas-oldval
      (swap! references (compare-and-set-swapfn name cas-oldval (:url ctx)))
      (swap! references assoc name (:url ctx)))))



(defresource reference [name]
  :available-media-types ["*/*"]
  :allowed-methods [:put :get]
  :can-put-to-missing? true
  :known-content-type? known-content-type?
  :processable? processable?
  :etag-matches-for-if-match? true
  :exists? (fn [ctx] (exists? ctx name))
  :handle-ok (fn [ctx] (handle-deref ctx name))
  :handle-exception handle-exception
  :put! (fn [ctx] (put! ctx name)))
