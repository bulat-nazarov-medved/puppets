(ns puppets.server.core
  (:use
   [puppets.server
    behavior
    constants
    builder
    database
    loginout
    mailer
    mutations
    signin
    world-info]
   [compojure core]
   [ring.middleware.edn])
  (:require
   [puppets.server.aes :as aes]
   [puppets.server.model :as m]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [noir.session :as session]
   [noir.util.middleware :as noir]
   [overtone.at-at :as at]
   [ring.middleware.session.cookie :as rc]
   [ring.util.response :refer (resource-response)])
  (:import
   [clojure.lang ExceptionInfo]))

(defn building-types []
  {:buildings
   (into
    {}
    (map
     (fn [[type desc]]
       [type (:description desc)])
     $buildings-description$))})

(defn attack-types []
  {:forces
   (mapv
    (fn [[type _]]
      type)
    $army-description$)
   :available-targets
   (mapv
    (fn [cell]
      [(:loc cell)
       (-> cell :village :name)])
    (filter
     (fn [cell]
       (:village cell))
     (vals (:cells @m/world))))})

(defn products-for-building [subtype]
  {:products
   (vec (keys (get $buildings-production$ subtype)))})

(defn warriors-for-building [subtype]
  {:warriors
   (case subtype
     :class [:object :metaobject])})

(defn authenticated-api-call
  [action params]
  (if (session/get :user)
    (case action
      "logout" (logout)
      "world" (print-world) ;;; for testing purposes
      "world-agent" (print-world-agent) ;;; for testing purposes
      "step" (do (recalc-world!) {}) ;;; for testing purposes
      "village-info" (if (:loc params)
                       (village-info (read-string (:loc params)))
                       (village-info nil))
      "building-types" (building-types)
      "attack-types" (attack-types)
      "add-building" (add-building (session/get :user)
                                   (read-string (:buildingtype params))
                                   (read-string (:loc params))
                                   (read-string (:village-loc params)))
      "add-war-order" (add-war-order (session/get :user)
                                     (read-string (:village-loc params))
                                     (read-string (:target params))
                                     (into
                                      {}
                                      (map
                                       (fn [[key value]]
                                         [key (Integer/parseInt value)])
                                       (filter
                                        (fn [[key value]]
                                          (and (contains? $army-description$ key)
                                               (try
                                                 (Integer/parseInt value)
                                                 (catch Exception e
                                                   nil))))
                                        params))))
      "products-for-building" (products-for-building (read-string (:subtype params)))
      "warriors-for-building" (warriors-for-building (read-string (:subtype params)))
      "add-training-order" (add-training-order (session/get :user)
                                               (read-string (:building-id params))
                                               (read-string (:warrior params))
                                               (:quantity params))
      "add-production-order" (add-production-order (session/get :user)
                                                   (read-string (:building-id params))
                                                   (read-string (:product params))
                                                   (:quantity params)))
    (throw (ex-info "Authentication exception."
                    {:type :auth
                     :action action
                     :params params}))))

(defn api-call
  [action params]
  (case action
    "login" (login params)
    "signin" (signin params)
    (authenticated-api-call action params)))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "text/plain"}
   :body (pr-str data)})

(defroutes api-routes
  (GET "/:action" [action & params]
       (try
         (generate-response (assoc (api-call action params) :success true))
         (catch ExceptionInfo ei
           (let [edata (ex-data ei)]
             (case (:type edata)
               :auth (generate-response (assoc edata :success false) 401))))
         (catch Throwable t
           (generate-response {:success false :error (str t)} 500)))))

(defn session-js-response []
  {:status 200
   :headers {"Content-Type" "application/javascript"}
   :body (str "var userlogged = " (if (session/get :user) true false) ";")})

(defn activate-code [code]
  (let [user (user-for-activation code)]
    (if user
      (do (activate-user! user)
          (m/activate-user! (:id user))
          (generate-response "You successfully activated. Please, login."))
      (generate-response "Activation error."))))

(defroutes app-routes
  (context "/api" [] api-routes)
  (GET "/" [] (resource-response "blank.html" {:root "public"}))
  (GET "/activate/:code" [code] (activate-code code))
  (GET "/js/session.js" [] (session-js-response))
  (route/resources "/")
  (route/not-found "Not found"))

(def app (noir/app-handler
          [(-> app-routes
               wrap-edn-params
               handler/site)]
          :session-options {:cookie-name "puppets"
                            :store (rc/cookie-store)}))

(defn world-init []
  (build-world!)
  (doseq [user (users)]
    (m/create-user! (:id user) (:login user)
                    (:password user) (:active user))))

(def tp (at/mk-pool))

(defn init []
  (define-database)
  (upgrade-to-latest)
  (world-init)
  (at/every 1000 #(send m/world recalc-world) tp))
