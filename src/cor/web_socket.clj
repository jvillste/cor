(ns cor.web-socket
  (:require [taoensso.timbre :as timbre]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as http-kit-adapter]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [clojure.core.async :as async]))

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
