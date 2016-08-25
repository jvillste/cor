(ns cor.state
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :as async]))



(defn start-transaction-loop [state-atom]
  (async/go-loop []
    (when-let [command (async/<! (:transaction-channel @state-atom))]
      (swap! state-atom command)
      (recur))))

(defn create-state-atom []
  (let [state-atom (reagent/atom {:transaction-channel (async/chan)})]
    (start-transaction-loop state-atom)
    state-atom))

(defn apply-to-state [transaction-channel function & arguments]
  (async/put! transaction-channel
              (fn [state]
                (apply function state arguments))))

(defn get-page-state [state page-key]
  (get-in state
          [:page-state page-key]))

(defn update-in-page-state [state page-key path function & arguments]
  (apply update-in
         state
         (concat [:page-state page-key] path)
         function
         arguments))

(defn apply-to-page-state [transaction-channel page-key function & arguments]
  (async/put! transaction-channel
              (fn [state]
                (apply update-in-page-state
                       state
                       page-key
                       []
                       function
                       arguments))))

(defn apply-assoc-to-page-state [transaction-channel page-key & kvs]
  (async/put! transaction-channel
              (fn [state]
                (apply update-in-page-state
                       state
                       page-key
                       []
                       assoc
                       kvs))))

(defn assoc-page-state [state page-key & kvs]
  (apply update-in-page-state state page-key [] assoc kvs))


