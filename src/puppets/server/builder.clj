(ns puppets.server.builder
  (:use
   [puppets.server constants model]))

(defn build-world! []
  (clear-world!)
  (doseq [x (range dim)
          y (range dim)]
    (create-cell! [x y]))
  nil)

(defn build-new-user! [name]
  (let [user (create-user! name)]
    (create-village! [0 0] (:id user))))

(defn test-build! []
  (build-world!)
  (build-new-user! "user1")
  (create-building! :resource :cpufreqd 1 [0 0] [0 0])
  (create-building! :production :throws 2 [0 0] [0 0])
  nil)
