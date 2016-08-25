(ns cor.debug
  (:require [cor.db :as db]))


(defn database-statement-table [statements]
  (when (not (empty? statements))
    [:table {:class "statement-table"}
     [:tbody (for [[e a v] (sort-by #(vec (take 2 %))
                                    statements)]
               [:tr {:key [e a v]}
                [:td (str e)]
                [:td (str a)]
                [:td (str v)]])]]))

(defn transaction-statement-table [statements]
  (when (not (empty? statements))
    [:table {:class "statement-table"}
     [:tbody (for [[c e a v] (sort-by (fn [statement]
                                        [(nth statement 1)
                                         (nth statement 2)])
                                      statements)]
               [:tr {:key [c e a v]}
                [:td (str c)]
                [:td (str e)]
                [:td (str a)]
                [:td (str v)]])]]))


(defn debug-view [state]
  [:div
   [:h1 "Conflicts"]

   [:pre (pr-str (:conflicting-datoms state))]
   
   [:h1 "Transaction"]

   [transaction-statement-table (db/db-diff (:source-db state) (:db state))]

   [:h1 "Database"]

   [database-statement-table (db/db-attributes (:source-db state))]])
