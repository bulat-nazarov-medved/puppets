(ns puppets.server.core
  (:use
   [compojure core]
   [ring.middleware.edn])
  (:require
   [compojure.handler :as handler]
   [compojure.route :as route]
   [noir.session :as session]
   [noir.util.middleware :as noir]
   [ring.middleware.session.cookie :as rc]
   [ring.util.response :refer (resource-response)]))

(defn authenticated-api-call
  [action params]
  (if (session/get :user)
    (case action
      )
    (throw (Exception. "Not authorized."))))

(defn api-call
  [action params]
  (case action
    "login" {}
    (authenticated-api-call action params)))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "text/plain"}
   :body (pr-str data)})

(defroutes api-routes
  (GET "/:action" [action & params]
       (try
         (generate-response (assoc (api-call action params)
                              :success true))
         (catch Throwable t
           (generate-response {:success false :error t} 401)))))

(defroutes app-routes
  (context "/api" [] api-routes)
  (GET "/" [] (resource-response "blank.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "Not found"))

(def app (noir/app-handler
          [(-> app-routes
               wrap-edn-params
               handler/site)]
          :session-options {:cookie-name "puppets"
                            :store (rc/cookie-store)}))
