(ns cor.db
  (:require [datascript.core :as d]
            [datascript.db :as db]
            [clojure.set :as set]
            [cljs.test :refer-macros [deftest is testing run-tests]]))

(defn reference-attributes [schema]
  (->> schema
       (filter (fn [[k v]] (= :db.type/ref (:db/valueType v))))
       (map first)
       set))

(defn cardinality-many-attributes [schema]
  (->> schema
       (filter (fn [[k v]] (= :db.cardinality/many (:db/cardinality v))))
       (map first)
       set))

(defn ids [db]
  (let [reference-attribute-set (reference-attributes (:schema db))]
    (reduce (fn [result [e a v t]]
              (-> result
                  (conj e)
                  (cond-> (reference-attribute-set a)
                    (conj v))))
            #{}
            (d/datoms db :eavt))))

(deftest ids-test
  (let [conn (d/create-conn {:queue/title {}
                             :queue/first-task {:db/valueType :db.type/ref}
                             :task/title {}})]
    (d/transact! conn
                 [[:db/add -1 :queue/title "first"]
                  [:db/add -1 :queue/first-task -2]])
    (is (= #{1 2}
           (ids @conn)))))

(defn db-statements [db]
  (reduce (fn [datoms [e a v t added]]
            (conj datoms [e a v]))
          #{}
          (d/datoms db :eavt)))

(defn db-diff [db1 db2]
  (let [reference-attribute-set (reference-attributes (:schema db1))
        cardinality-many-attribute-set (cardinality-many-attributes (:schema db1))
        db1-ids (ids db1)
        db1-statements (db-statements db1)
        db2-statements (db-statements db2)
        transaction-statement (fn [c e a v]
                                [c
                                 (if (db1-ids e)
                                   e
                                   (- 0 e))
                                 a
                                 (if (reference-attribute-set a)
                                   (if (db1-ids v)
                                     v
                                     (- 0 v))
                                   v)])
        added-statements (reduce (fn [statements [e a v]]
                                   (conj statements
                                         (transaction-statement :db/add e a v)))
                                 []
                                 (set/difference db2-statements
                                                 db1-statements))]
    (concat added-statements
            (reduce (fn [statements [e a v]]
                      (if (or (cardinality-many-attribute-set a)
                              (not (some (fn [[c2 e2 a2 v2]]
                                           (and (= e e2)
                                                (= a a2)))
                                         added-statements)))
                        (conj statements
                              (transaction-statement :db/retract e a v))
                        statements))
                    []
                    (set/difference db1-statements
                                    db2-statements)))))

(deftest db-diff-test
  (let [db1 (d/db-with (d/empty-db {:queue/title {}
                                    :queue/first-task {:db/valueType :db.type/ref}
                                    :task/title {}})
                       [[:db/add -1 :queue/title "first"]
                        [:db/add -1 :queue/first-task -2]])]

    (is (= [[:db/add 1 :queue/title "first2"]
            [:db/add -3 :queue/first-task 2]
            [:db/add -3 :queue/title "second"]
            [:db/retract 1 :queue/first-task 2]]
           (db-diff db1
                    (-> db1
                        (d/db-with [[:db/add 1 :queue/title "first2"]
                                    [:db/retract 1 :queue/first-task 2]
                                    [:db/add -1 :queue/title "second"]])
                        (d/db-with [[:db/add 3 :queue/first-task 2]])))))))

(defn max-tx [db e a]
  (let [txs (->> (d/datoms db :eavt e a)
                 (map :tx))]
    (if (seq txs)
      (apply max txs)
      nil)))

(defn db-diff-with-old-txs [db1 db2]
  (let [reference-attribute-set (reference-attributes (:schema db1))
        cardinality-many-attribute-set (cardinality-many-attributes (:schema db1))
        db1-ids (ids db1)
        db1-statements (db-statements db1)
        db2-statements (db-statements db2)
        transaction-statement (fn [c e a v tx]
                                [c
                                 (if (db1-ids e)
                                   e
                                   (- 0 e))
                                 a
                                 (if (reference-attribute-set a)
                                   (if (db1-ids v)
                                     v
                                     (- 0 v))
                                   v)
                                 tx])
        added-statements (reduce (fn [statements [e a v tx added]]
                                   (conj statements
                                         (transaction-statement :db/add e a v (max-tx db1 e a))))
                                 []
                                 (set/difference db2-statements
                                                 db1-statements))]
    (concat added-statements
            (reduce (fn [statements [e a v]]
                      (if (or (cardinality-many-attribute-set a)
                              (not (some (fn [[c2 e2 a2 v2]]
                                           (and (= e e2)
                                                (= a a2)))
                                         added-statements)))
                        (conj statements
                              (transaction-statement :db/retract e a v  (max-tx db1 e a)))
                        statements))
                    []
                    (set/difference db1-statements
                                    db2-statements)))))

(deftest db-diff-with-old-txs-test
  (let [schema {:queue/title {},
                :queue/first-task {:db/valueType :db.type/ref},
                :task/title {}}]
    (is (= '([:db/add 1 :queue/title "first2" 10])
           (db-diff-with-old-txs (d/init-db [(d/datom 1 :queue/title "first" 10)
                                             (d/datom 2 :queue/title "second" 10)]
                                            schema)
                                 (d/init-db [(d/datom 1 :queue/title "first2" 11)
                                             (d/datom 2 :queue/title "second" 10)]
                                            schema))))

    (is (= '([:db/add -3 :queue/title "third" nil])
           (db-diff-with-old-txs (d/init-db [(d/datom 1 :queue/title "first" 10)
                                             (d/datom 2 :queue/title "second" 10)]
                                            schema)
                                 (d/init-db [(d/datom 1 :queue/title "first" 10)
                                             (d/datom 2 :queue/title "second" 10)
                                             (d/datom 3 :queue/title "third" 11)]
                                            schema))))

    (is (= '([:db/retract 2 :queue/title "second" 1])
           (db-diff-with-old-txs (d/init-db [(d/datom 1 :queue/title "first" 1)
                                             (d/datom 2 :queue/title "second" 1)]
                                            schema)
                                 (d/init-db [(d/datom 1 :queue/title "first" 1)]
                                            schema))))))



(defn db-attributes [db]
  (map #(take 3 %)
       (d/datoms db :eavt)))

(defn entity-attributes [db entity]
  (map #(take 3 %)
       (d/datoms db :eavt entity)))


(defn entity-referring-attributes [db entity]
  (d/q '[:find ?e ?a ?entity
         :in $ ?entity
         :where [?e ?a ?entity]]
       db entity))



