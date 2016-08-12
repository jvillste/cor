(ns cor.log
  (:require [taoensso.timbre :as timbre]))

(defmacro info [& messages]
  `(timbre/info ~@messages))
