(ns cor.api
  (:require [taoensso.timbre :as timbre]
            [compojure.core :as compojure]
            [ring.middleware.multipart-params :as multipart-params]))

(defn dispatch-command [body state-atom api-namespace]
  (try (let [[command & arguments] body]
         (timbre/info "handling " (pr-str body))
         (let [result (if-let [function-var (get (ns-publics api-namespace) (symbol (name command)))]
                        (if (:cor/api (meta function-var))
                          (apply @function-var (concat [state-atom] arguments))
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

(defn hanadle-post [body state-atom api-namespace]
  (-> body
      slurp
      read-string
      (dispatch-command state-atom api-namespace)
      pr-str))

(defn api-routes [path initial-state api-namespace]
  (let [state-atom (atom initial-state)]
    [(compojure/POST path {body :body} (hanadle-post body
                                                     state-atom
                                                     api-namespace))]))



(defn file-post-route [path file-handler]
  (multipart-params/wrap-multipart-params
   (compojure/POST path
                   request
                   (-> (file-handler (get (:params request) "file")
                                     (read-string (get (:params request) "command")))
                       pr-str))))


