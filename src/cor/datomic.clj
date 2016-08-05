(ns cor.datomic
  (:require [datomic.api :as d]
            [agile-queue-common.core :as common]
            [datascript.db :as datascript-db]
            [cor.datomic-pull-api :as datomic-pull-api])
  (:use clojure.test))

(def schema (map (fn [attribute]
                   (assoc attribute :db/id (d/tempid :db.part/db)))
                 common/schema))

(defn ref? [db attribute]
  (-> (d/entity db attribute)
      :db/valueType
      (= :db.type/ref)))

(defn component? [db attribute]
  (-> (d/entity db attribute)
      :db/isComponent))

(defn multival? [db attribute]
  (= :db.cardinality/many
     (-> (d/entity db attribute)
         :db/cardinality)))


(defn set-tempid-partition [id]
  (println id)
  (if (< id 0)
    (d/tempid :db.part/user id)
    id))

#_ (set-tempid-partition -1)

(defn set-tempid-partitions [transaction-statement db]
  (println transaction-statement)
  (-> transaction-statement
      (update-in [1] set-tempid-partition)
      (cond-> (ref? db
                    (nth transaction-statement
                         2))
        (update-in [3] set-tempid-partition))))


(defn new-memory-db-uri []
  (str "datomic:mem://" (int (rand 100000))))

#_(def db-uri "datomic:free://localhost:4334/apartments2")

(defn create-database [uri]
  (d/delete-database uri)
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (d/transact conn schema)))


#_(let [uri (new-memory-db-uri)]
    (create-database uri)
    (let [conn (d/connect uri)]
      (d/transact conn
                  [[:db/add 0 :queue/first-task 1]
                   [:db/add 1 :task/next-task 2]
                   [:db/add 1 :task/title "Haa"]
                   [:db/add 2 :task/next-task 3]
                   [:db/add 3 :task/next-task 4]])

      (d/transact conn
                  [[:db/add 1 :task/title "Haa2"]])

      (let [attribute (d/entity (d/db conn)
                                :task/next-task)]
        (println (:db/valueType attribute)))
      
      #_(let [task (d/entity (d/db conn)
                             1)]
          (println (:task/title task)))

      #_(let [db (d/db conn)]
          (->> (d/datoms db :eavt 1)
               (into [])
               (println))))
    
    (d/delete-database uri))


(defn transact [db-uri transaction]
  (let [conn (d/connect db-uri)
        db (d/db conn)]
    (d/transact conn
                #_[[:db/add #db/id[:db.part/user -1] :queue/key "1"]]
                (map (fn [transaction-statement]
                       (set-tempid-partitions transaction-statement
                                              db))
                     transaction))))

(defn datomic-datom-to-datascript-datom [datomic-datom db-uri]
  (datascript-db/->Datom (:e datomic-datom)
                         (-> (d/entity (d/db (d/connect db-uri))
                                       (:a datomic-datom))
                             :db/ident)
                         (:v datomic-datom)
                         (:tx datomic-datom)
                         (:added datomic-datom)))

(defn pull [pattern id db-uri]
  (->> (datomic-pull-api/pull-datoms (d/db (d/connect db-uri))
                                     pattern
                                     id)
       (map (fn [datomic-datom]
              (datomic-datom-to-datascript-datom datomic-datom
                                                 db-uri)))))
