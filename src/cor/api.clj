(ns cor.api
  (:require [taoensso.timbre :as timbre]
            [compojure.core :as compojure]))

(defn dispatch-command [body api-namespace]
  (try (let [[command & arguments] body]
         (timbre/info "handling " (pr-str body))
         (let [result (if-let [function-var (get (ns-publics api-namespace) (symbol (name command)))]
                        (if (:cor/api (meta function-var))
                          (apply @function-var arguments)
                          (str "unknown command: " command))
                        (str "unknown command: " command))]

           (let [result-message (pr-str result)]
             (timbre/info "result " (subs result-message 0 (min (.length result-message)
                                                                300))))
           
           result))
       (catch Exception e
         (timbre/info "Exception in handle- post" e)
         (.printStackTrace e *out*)
         (throw e))))

(defn hanadle-post [body api-namespace]
  (-> body
      slurp
      read-string
      (dispatch-command api-namespace)
      pr-str))

(defn api-route [path api-namespace]
  (compojure/POST path {body :body} (hanadle-post body
                                                  api-namespace)))
