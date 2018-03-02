(ns cor.client
  (:require (cor [api :as api])))

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

(defn define-in-process-method-for-api-var [client-class client-ns client-var]
  (.addMethod @(get (ns-publics client-ns)
                    (:name (meta client-var)))
              client-class
              (fn [this & arguments]
                (apply @(::api-var (meta client-var))
                       (:state-atom this)
                       arguments))))

(defmacro define-in-process-client
  ([client-class-symbol]
   (define-in-process-client client-class-symbol *ns*))
  
  ([client-class-symbol client-namespace]
   `(do 
      (defrecord ~client-class-symbol [~'state-atom])
       
      (doseq [client-var# (client-multimethods ~client-namespace)]
        (define-in-process-method-for-api-var ~client-class-symbol ~client-namespace client-var#)))))


