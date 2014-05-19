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
  (create-building! :building :cpufreqd 10 [0 0] [0 0])
  (let [building (create-building! :production :throws 2 [0 0] [0 0])]
    (place-production-order! (:id building) :null-pointer-exception 10)
    (place-production-order! (:id building) :stack-overflow-exception 5))
  (let [building (create-building! :military :class 10 [0 0] [0 0])]
    (place-training-order! (:id building) :object 4)
    (place-training-order! (:id building) :metaobject 2))
  nil)
