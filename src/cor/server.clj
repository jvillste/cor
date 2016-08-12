(ns cor.server
  (:require [org.httpkit.server :as http-kit]
            [taoensso.timbre :as timbre]))

(def server (atom nil))

(defn start-server [app port]
  (timbre/info "starting")
  (when @server (@server))
  (.start (Thread. (fn [] (reset! server
                                  (http-kit/run-server app
                                                       {:port port}))))))
