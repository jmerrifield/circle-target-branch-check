(ns target-branch-checker.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [cemerick.url :refer [url]]
            [clojure.data.json :as json]))

(defn- last-circle-build [repo-fullname branch]
  (->
   (url "https://circleci.com/api/v1/project" repo-fullname "tree" branch)
   str
   (#(http/get (str %) {:headers {"Accept" "application/json"}
                        :query-params {:circle-token (env :circle-token) :limit 1 :filter "completed"}}))
   deref
   :body
   (#(json/read-str % :key-fn keyword))
   first))

;; (last-circle-build "change/fe" "master")

(defn- create-status [outcome build-url branch]
  (let [failed (not= outcome "success")]
    {:state (if failed "failure" "success")
     :target_url build-url
     :context "target-branch-checker"
     :description (str "The target branch (" branch ") has a " (if failed "red" "green") " build")}))

;; (create-status "failed" "http://example.org/builds/1" "master")

(defn- get-open-prs [owner repo base]
  (->
   (url "https://api.github.com/repos" owner repo "pulls")
   str
   (#(http/get % {:query-params {:state "open" :base base}
                  :headers {"Authorization" (str "token " (env :gh-token))}}))
   deref
   :body
   (#(json/read-str % :key-fn keyword))))

;; (get-open-prs "change" "fe" "master")

(defn- post-status [url status]
  (http/post url {:headers {"Authorization" (str "token " (env :gh-token))}
                  :body (json/write-str status)}))

(defroutes app-routes
  (GET "/" []  "Hello World")

  (POST "/payload/github" {{action :action 
                            {{branch :ref} :base statuses-url :statuses_url} :pull_request
                            {repo-fullname :full_name} :repository} 
                           :body}
        (prn "GH payload" action branch statuses-url repo-fullname)
        (cond 
         (= action "opened")
         (let [{:keys [build_url outcome]} (last-circle-build repo-fullname branch)] 
           (prn "Creating status" action branch statuses-url repo-fullname build_url outcome)
           (post-status statuses-url (create-status outcome build_url branch))))
        {:status 200})

  (POST "/payload/circle" {{{:keys [branch username reponame outcome build_url]} :payload} :body}
        (prn "Circle payload" branch username reponame outcome build_url)
        (doseq [{statuses-url :statuses_url} (get-open-prs username reponame branch)]
          (post-status statuses-url (create-status outcome build_url branch)))
        {:status 200})

  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))
