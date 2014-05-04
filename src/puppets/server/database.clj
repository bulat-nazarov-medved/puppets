(ns puppets.server.database
  (:use
   [korma core db])
  (:require
   [clojure.java.io :as io]
   [clojure.java.jdbc.deprecated :as sql]
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

(def structure-version 1)

(defmacro with-exception-redirect [& body]
  `(try
     ~@body
     (catch Exception e#
       )))

;;; Entities

(defentity dbkey
  (table "keys")
  (pk :key))

(defentity user
  (table "users")
  (pk :id))

;; DB functions

(defn find-key
  [key]
  (with-exception-redirect
    (:value (first (select dbkey (where {:key key}))))))

;;;DB upgrade/downgrade

(defn next-version [version]
  (if version
    (+ version 1)
    0))

(defn previous-version [version]
  (if (or (not version) (= version 0))
    nil
    (- version 1)))

(defn execute-sql-file
  [sql-resource]
  (sql/with-connection (get-connection db)
    (sql/transaction (sql/do-prepared (slurp (io/resource sql-resource))))))

(defn structure-upgrade-step
  [version-from]
  (let [upgrade-file (str "sql/"
                          (or version-from "nil") "-" (next-version version-from)
                          "/upgrade.sql")]
    (execute-sql-file upgrade-file)))

(defn structure-downgrade-step
  [version-from]
  (let [downgrade-file (str "sql/"
                            (or (previous-version version-from) "nil") "-" version-from
                            "/downgrade.sql")]
    (execute-sql-file downgrade-file)))

(defn perform-structure-steps
  [version step-function]
  (let [current-version (find-key "version")]
    (when (not= version current-version)
      (step-function current-version)
      (perform-structure-steps version step-function))))

(defn upgrade-structure-to
  [version]
  (perform-structure-steps version structure-upgrade-step))

(defn downgrade-structure-to
  [version]
  (perform-structure-steps version structure-downgrade-step))

(defn upgrade-to-latest []
  (upgrade-structure-to structure-version))
