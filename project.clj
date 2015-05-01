(defproject target-branch-checker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.3"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-json "0.3.1"]
                 [http-kit "2.1.16"]
                 [environ "0.5.0"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[lein-ring "0.9.3"]]
  :ring {:handler target-branch-checker.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
