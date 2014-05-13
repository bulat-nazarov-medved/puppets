(ns puppets.server.database
  (:use
   [korma core db])
  (:require
   [clojure.java.io :as io]
   [clojure.java.jdbc.deprecated :as sql]
   [clojure.string :as str]
   [crypto.password.scrypt :as psw]
   [puppets.server.korma :as pk])
  (:import
   [java.net URI]))

(defn define-database []
  (let [db-uri (URI. (System/getenv "DATABASE_URL"))
        userinfo (str/split (.getUserInfo db-uri) #":")
        username (first userinfo)
        password (second userinfo)]
    (defdb db (pk/postgres
               {:host (.getHost db-uri)
                :port (.getPort db-uri)
                :db (subs (.getPath db-uri) 1)
                :user username
                :password password
                :props {:ssl true
                        :sslfactory "org.postgresql.ssl.NonValidatingFactory"}}))))

(def structure-version 2)

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

(defn create-id-pool [pool-name pool-size]
  (let [available-id (atom 0)
        reserved-to-id (atom 0)
        lock (new Object)]
    (fn
      ([action]
         (case action
           :reset (do (reset! available-id 0)
                      (reset! reserved-to-id 0))))
      ([]
         (locking lock
           (when (= @available-id @reserved-to-id)
             (let [current-aid (find-key pool-name)
                   lease-aid (+ current-aid pool-size)]
               (update dbkey (set-fields {:value lease-aid})
                       (where {:key pool-name}))
               (reset! available-id current-aid)
               (reset! reserved-to-id lease-aid)))
           (let [return-id @available-id]
             (reset! available-id (inc @available-id))
             return-id))))))

(def general-pool (create-id-pool "general_pool" 10))

(defn user-exists?
  [username]
  (= 1 (count (select user (where {:login username})))))

(defn user-for-activation
  [code]
  (first (select user (where {:activation_code code}))))

(defn user-create!
  [username email password race activation-code]
  (let [id (general-pool)]
    (insert user (values {:id id :login username :email email
                          :password (psw/encrypt password)
                          :race race :activation_code activation-code
                          :active false :email_sent false}))
    id))

(defn user-mark-email-sent! [user-id]
  (update user (set-fields {:email_sent true})))

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
  (println version-from)
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
