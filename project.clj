(defproject cor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [com.taoensso/timbre "4.7.3"]
                 [http-kit "2.1.18"]
                 [ring-cors "0.1.8"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [com.datomic/datomic-free "0.9.5372"]
                 [com.taoensso/sente "1.9.0"]])
