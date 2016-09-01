(ns cor.server
  (:require [org.httpkit.server :as http-kit]
            [taoensso.timbre :as timbre]))

(def server (atom nil))

(defn start-server [app port]
  (when @server
    (do (timbre/info "closing")
        (@server)
        (Thread/sleep 1000)))
  (timbre/info "starting")
  (.start (Thread. (fn [] (reset! server
                                  (http-kit/run-server app
                                                       {:port port}))))))
