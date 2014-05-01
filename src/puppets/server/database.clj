(ns puppets.server.database
  (:use
   [korma core db])
  (:require
   [clojure.string :as str]
   [puppets.server.korma :as pk])
  (:import
   [java.net URI]))

(let [db-uri (URI. (System/getenv "DATABASE_URL"))
      userinfo (str/split (.getUserInfo db-uri) #":")
      username (first userinfo)
      password (second userinfo)]
  (defdb db (pk/postgres {:host (.getHost db-uri)
                          :port (.getPort db-uri)
                          :db (subs (.getPath db-uri) 1)
                          :user username
                          :password password
                          :props {:ssl true
                                  :sslfactory "org.postgresql.ssl.NonValidatingFactory"}})))

(defentity users
  (table "users")
  (pk "id"))

