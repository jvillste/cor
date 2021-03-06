(ns cor.api
  (:require [clojure.java.io :as io]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [cor.web-socket :as web-socket]
            [logga.core :as logga]
            [ring.middleware.cors :as cors]
            [ring.middleware.multipart-params :as multipart-params]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre]))

(defn api-vars [api-namespace]
  (->> (ns-publics api-namespace)
       (vals)
       (filter (fn [var]
                 (:cor/api (meta var))))))

(defn unknown-command-response [command]
  (response/not-found (pr-str {:error "unknown command"
                               :command command})))

(defn command-response [result]
  (if (bytes? result)
    (-> (response/response (io/input-stream result))
        (response/header "Content-Type" "bytes"))
    (-> (response/response (pr-str result))
        (response/header "Content-Type" "edn"))))

(defn dispatch-command [body state-atom api-namespace]
  (try (let [[command & arguments] body]
         (logga/write "handling " (pr-str body))
         (let [result (if-let [function-var (get (ns-publics api-namespace) (symbol (name command)))]
                        (if (:cor/api (meta function-var))
                          (command-response (apply @function-var (concat [state-atom] arguments)))
                          (if (:cor.api/stateles (meta function-var))
                            (command-response (apply @function-var arguments))
                            (unknown-command-response command)))
                        (unknown-command-response command))]

           (let [result-message (pr-str result)]
             (logga/write "result " (subs result-message 0 (min (.length result-message)
                                                                300))))
           result))
       (catch Exception e
         (timbre/info "Exception in handle- post" e)
         (.printStackTrace e *out*)
         (throw e))))

(defn safely-read-string [string]
  (binding [*read-eval* false]
    (read-string string)))

(defn hanadle-post [body state-atom api-namespace]
  (-> body
      slurp
      safely-read-string
      (dispatch-command state-atom api-namespace)))

(defn api-routes [path initial-state api-namespace]
  (let [state-atom (atom initial-state)]
    [(compojure/POST path {body :body} (hanadle-post body
                                                     state-atom
                                                     api-namespace))]))


(defn app [initial-state api-namespace]
  (apply compojure/routes
         (concat (api-routes "/api"
                             initial-state
                             api-namespace)
                 [(compojure/GET "/api" [] "api")
                  (route/resources "/")
                  (route/not-found "Not Found")])))

(defn wrap-cors [routes access-control-allow-origin access-control-allow-methods]
  (cors/wrap-cors routes
                  :access-control-allow-origin access-control-allow-origin
                  :access-control-allow-methods access-control-allow-methods))

(defn app-with-web-socket [initial-state  api-namespace]
  (let [channel-socket (web-socket/create-channel-socket)]
    (-> (apply compojure/routes
               (concat (api-routes "/api"
                                   (conj initial-state
                                         {:channel-socket channel-socket})
                                   api-namespace)
                       (web-socket/routes "/chsk" channel-socket)
                       [(route/resources "/")
                        (route/not-found "Not Found")]))
        (web-socket/wrap))))

(defn file-post-route [path file-handler]
  (multipart-params/wrap-multipart-params
   (compojure/POST path
                   request
                   (-> (file-handler (get (:params request) "file")
                                     (safely-read-string (get (:params request) "command")))
                       pr-str))))


