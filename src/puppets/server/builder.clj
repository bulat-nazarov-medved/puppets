(ns puppets.server.builder
  (:use
   [puppets.server constants model]))

(defn build-world! []
  (clear-world!)
  (doseq [x (range dim)
          y (range dim)]
    (create-cell! [x y]))
  nil)

(defn build-new-user! [id name loc]
  (let [user (create-user! id name "" true)]
    (create-village! loc (:id user))))

(defn test-build! []
  (build-world!)
  (build-new-user! 0 "user1" [0 0])
  (build-new-user! 1 "user2" [4 4])
  (create-building! :resource :cpufreqd 1 [0 0] [0 0])
  (create-building! :building :cpufreqd 10 [0 0] [0 0])
  (let [building (create-building! :production :throws 2 [0 0] [0 0])]
    (place-production-order! (:id building) :null-pointer-exception 10)
    (place-production-order! (:id building) :stack-overflow-exception 5))
  (let [building (create-building! :military :class 10 [0 0] [0 0])]
    (place-training-order! (:id building) :object 4)
    (place-training-order! (:id building) :metaobject 2))
  (let [building (create-building! :military :class 10 [4 4] [4 4])]
    (place-training-order! (:id building) :object 8))
  (place-war-order! [0 0] [4 4] {:object 4})
  nil)
