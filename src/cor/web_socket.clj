(ns cor.web-socket
  (:require [taoensso.timbre :as timbre]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as http-kit-adapter]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [clojure.core.async :as async]
            [compojure.core :as compojure]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]))

(defn user-id [req]
  (:client-id req))


(defn create-channel-socket []
  (let [channel-socket (sente/make-channel-socket! (http-kit-adapter/get-sch-adapter)
                                                   {:user-id-fn #'user-id})]
    
    (async/go-loop []
      (let [message (async/<! (:ch-recv channel-socket))]
        (println "Message:" (:id message) (:uid message) (:?data message))
        (recur)))
    
    channel-socket))


(defn broadcast-message [channel-socket messsage-id data]
  (doseq [uid (:any @(:connected-uids channel-socket))]
    ((:send-fn channel-socket) uid [messsage-id data])))


(defn routes [path channel-socket]
  [(compojure/GET  path req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
   (compojure/POST path req ((:ajax-post-fn channel-socket)                req))])


(defn wrap [app]
  (-> app
      keyword-params/wrap-keyword-params
      params/wrap-params))
