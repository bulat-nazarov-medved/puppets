(ns puppets.server.loginout
  (:require
   [puppets.server.model :as m]
   [noir.session :as session]))

(defn logout []
  (session/remove! :user)
  {:status :success})

(defn login [{:keys [username password]}]
  (if (session/get :user)
    {:status :error :description "Please logout first"}
    (let [user (m/search-user! username password)]
      (if user
        (do (m/user-check-village! (:id user))
            (session/put! :user (:id user))
            {:status :success})
        {:status :error :description "Username/password not found"}))))
