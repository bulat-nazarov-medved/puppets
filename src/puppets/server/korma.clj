(ns puppets.server.korma
  (:require [clojure.string :as str]))

(defn postgres
  "Create a database specification for a postgres database. Opts should include
keys for :db, :user, and :password. You can also optionally set host and
port."
  [{:keys [host port db make-pool? props]
    :or {host "localhost", port 5432, db "", make-pool? true, props {}}
    :as opts}]
  (merge {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :subname (str "//" host ":" port "/" db
                        (if (> (count props) 0)
                          (str "?" (str/join "&" (for [[k v] props]
                                                   (str (name k) "=" v))))
                          ""))
          :make-pool? make-pool?}
         opts))
