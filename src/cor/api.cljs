(ns cor.api
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cljs.pprint :as pprint]
            [cljs-http.client :as http]
            [cljs.reader :as reader]
            [cor.state :as state])
  (:require-macros [cljs.core.async.macros :as async]))


(def file-url "/file")
(def api-url "/api")

(defn call-to-chan [body]
  (let [channel (async/chan)]
    (async/go (let [response (-> (async/<! (http/post api-url
                                                      {:edn-params body
                                                       :with-credentials? false}))
                                 (:body)
                                 (cljs.reader/read-string)
                                 (or :nil))]
                (async/>! channel response)
                (async/close! channel)))
    channel))


(defn post-file-to-chan [file-input-id command]
  (let [channel (async/chan)
        file (-> (.getElementById js/document file-input-id)
                 .-files
                 (aget 0))]
    (async/go (let [response (-> (async/<! (http/post file-url
                                                      {:multipart-params [["file" file]
                                                                          ["command" (pr-str command)]]
                                                       :with-credentials? false}))
                                 (:body)
                                 (cljs.reader/read-string)
                                 (or :nil))]
                (async/>! channel response)
                (async/close! channel)))
    channel))

(defn handle-result-from-chan [chan callback]
  (async/go (callback (async/<! chan))))


(defn call [body callback]
  (-> (call-to-chan body)
      (handle-result-from-chan callback)))

(defn post-file [file-input-id command callback]
  (-> (post-file-to-chan file-input-id command)
      (handle-result-from-chan callback)))


(defn call-and-apply-to-state [state command function]
  (call command
        (fn [result]
          (state/apply-to-state state (fn [state]
                                  (function result state))))))


