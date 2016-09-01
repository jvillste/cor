(ns cor.web-socket
  (:require [taoensso.sente  :as sente :refer (cb-success?)]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :as async]))


(defn create-channel-socket [path]
  (sente/make-channel-socket! path)
  ;; (def chsk       chsk)
  ;; (def ch-chsk    ) ; ChannelSocket's receive channel
  ;; (def chsk-send! send-fn) ; ChannelSocket's send API fn
  ;; (def chsk-state state)   ; Watchable, read-only atom
  )

(defn handle-messages [channel-socket handler]
  (async/go-loop []
    (let [message (async/<! (:ch-recv channel-socket))
          type (first (:?data message))
          arguments (second (:?data message))]

      (handler type arguments)
      (recur))))
