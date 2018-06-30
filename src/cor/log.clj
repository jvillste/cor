(ns cor.log
  (:require [taoensso.timbre :as timbre]))

(defmacro info [& messages]
  `(timbre/info ~@messages))

(defonce log-lock (Object.))
(defonce last-log-line-time (volatile! (System/currentTimeMillis)))

(defn write-2 [& messages]
  (locking log-lock
    (let [time (System/currentTimeMillis)]
      (apply println (concat [(format "%20s" (.getName (Thread/currentThread)))
                              (format "%10d" (- time
                                                @last-log-line-time))]
                             messages))
      (vreset! last-log-line-time time))))

(comment
  (do (println "----")
      (.start (Thread. (fn [] (Thread/sleep (rand-int 200))
                         (write-2 "hi 1"))
                       "my thread"))
      (.start (Thread. (fn [] (Thread/sleep (rand-int 200))
                         (write-2 "hi 2"))
                       "my thread")))

  )
