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

(defn- create-status [failed build-url branch]
  {:state (if failed "failure" "success")
   :target_url build-url
   :context "target-branch-checker"
   :description (str "The target branch (" branch ") has a " (if failed "red" "green") " build")})

;; (create-status true "http://example.org/builds/1" "master")

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

(comment 
  (doseq [{statuses-url :statuses_url} (get-open-prs "change" "fe" "master")]
    (prn "PR" statuses-url)))

(defroutes app-routes
  (GET "/" "Hello World")

  (POST "/payload/github" {{:keys [action] 
                            {{branch :ref} :base} :pull_request
                            {statuses-url :statuses_url} :pull_request
                            {repo-fullname :full_name} :repository} 
                           :body}
        (prn "GH payload" action branch statuses-url repo-fullname)
        (cond 
         (= action "opened")
         (let [{:keys [build_url failed]} (last-circle-build repo-fullname branch (env :circle-token))] 
           (prn "Creating status" action branch statuses-url repo-fullname build_url failed)
           (http/post statuses-url {:body (json/write-str (create-status failed build_url branch))
                                    :headers {"Authorization" (str "token " (env :gh-token))}})))
        {:status 200})

  (POST "/payload/circle" {{{:keys [branch username reponame failed build_url]} :payload} :body}
        (prn "Circle payload" branch username reponame failed build_url)
        (doseq [{statuses-url :statuses_url} (get-open-prs username reponame branch (env :gh-token))]
          (http/post statuses-url {:body (json/write-str (create-status failed build_url branch))
                                   :headers {"Authorization" (str "token " (env :gh-token))}}))
        {:status 200})

  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))
