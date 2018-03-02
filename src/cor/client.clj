(ns cor.client
  (:require (cor [api :as api])
            [clj-http.client :as client]))

(defn client-multimethods [client-namespace]
  (->> (ns-publics client-namespace)
       (vals)
       (filter (fn [var]
                 (::api-var (meta var))))))

(defn- api-function-arguments [api-function-var]
  (rest (first (:arglists (meta api-function-var)))))

(defn- defmulti-for-api-var [var]
  (let [this (gensym)]
    `(defmulti ~(:name (meta var)) {:arglists (quote ~[(into ['client] (api-function-arguments var))])
                                    ::api-var ~var}
       (fn ~(vec (into [this] (api-function-arguments var)))
         (type ~this)))))

(defmacro define-multimethods-for-api-namespace [api-namespace]
  `(do ~@(map defmulti-for-api-var
              (api/api-vars api-namespace))))


;; in process

(defn define-in-process-method-for-client-multimethod-var [client-class client-multimethod-var]
  (.addMethod @client-multimethod-var
              client-class
              (fn [this & arguments]
                (apply @(::api-var (meta client-multimethod-var))
                       (:state-atom this)
                       arguments))))

(defn emit-define-client [client-class-symbol client-namespace define-method]
  `(doseq [client-multimethod-var# (client-multimethods ~client-namespace)]
     (~define-method ~client-class-symbol client-multimethod-var#)))

(defmacro define-in-process-client [client-class-symbol client-namespace]
  `(do 
     (defrecord ~client-class-symbol [~'state-atom])
     ~(emit-define-client client-class-symbol
                       client-namespace
                       define-in-process-method-for-client-multimethod-var)))


;; http

(defn define-http-method-for-client-multimethod-var [client-class client-multimethod-var]
  (.addMethod @client-multimethod-var
              client-class
              (fn [this & arguments]
                (:body (client/post (:url this) {:body (pr-str (into [(:name (meta client-multimethod-var))]
                                                               arguments))})))))

(defmacro define-http-client [client-class-symbol client-namespace]
  `(do 
     (defrecord ~client-class-symbol [~'url])
     ~(emit-define-client client-class-symbol
                          client-namespace
                          define-http-method-for-client-multimethod-var)))
