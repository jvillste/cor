(ns cor.data-state
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cor.db :as db])
  (:require-macros [cljs.core.async.macros :as async]))


(defn transact [state transaction-statements]
  (assoc state
         :db (d/db-with (:db state)
                        transaction-statements)))

(defn cancel [state]
  (assoc state
         :db (:source-db state)))

(defn initialize-state [state schema]
  (let [db (d/empty-db schema)]
    (assoc state
           :db db
           :source-db db
           :source-ids (db/ids db))))


