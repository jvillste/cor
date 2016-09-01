(ns cor.string
  #?(:clj (:use clojure.test)
     :cljs (:require [cljs.test :refer-macros [deftest is testing run-tests]])))


(defn length [string]
  #?(:clj (.length string)
     :cljs (.-length string)))

(defn substring [string from to]
  (let [length (length string)]
    (subs string
          (min from
               length)
          (min to
               length))))

(deftest substring-test
  (is (= "ab"
         (substring "abc"
                    0
                    2)))

  (is (= "abc"
         (substring "abc"
                    0
                    10)))

  (is (= ""
         (substring "abc"
                    10
                    10))))

