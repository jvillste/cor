(ns cor.routing
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]))

;; from https://github.com/reagent-project/reagent-cookbook/tree/master/recipes/add-routing

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (println "dispatching to " (prn-str (.-token event)))
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn setup-routes []
  (secretary/set-config! :prefix "#")

  (hook-browser-navigation!))

