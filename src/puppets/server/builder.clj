(ns puppets.server.builder
  (:use
   [puppets.server constants model]))

(defn build-world!
  []
  (clear-world!)
  (doseq [x (range dim)
          y (range dim)]
    (create-cell! [x y]))
  nil)

(defn build-new-user!
  []
  )
