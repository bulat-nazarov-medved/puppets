(ns puppets.server.world-info
  (:require
   [puppets.server.model :as m]
   [noir.session :as session]))

(defn print-world []
  {:world @m/world})
