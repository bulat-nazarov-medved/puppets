(ns puppets.server.core
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer (resource-response)]))

(defroutes app-routes
  (GET "/" [] (resource-response "blank.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "Not found"))

(def app
  (-> app-routes
      handler/site))
