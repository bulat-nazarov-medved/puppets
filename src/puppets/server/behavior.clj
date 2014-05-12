(ns puppets.server.behavior
  (:use
   [puppets.server constants model]))

(defn cells-puppet-area [world pupet]
  )

(defn puppet-want-to-eat [world puppet]
  (assoc-in world [:puppets (:id puppet) :state] :want-to-eat))

(defn puppet-die [world puppet]
  (update-in world [:puppets] dissoc (:id puppet)))

(defn behave-hunger [world puppet]
  (let [hunger (:hunger puppet)]
    (cond
     (> hunger hunger-to-death) (puppet-die world puppet)
     (> hunger hunger-to-eat) (puppet-want-to-eat world puppet)
     :else world)))

(defn behave-eat [world puppet]
  (update-in world [:puppets (:id puppet) :hunger] inc))

(defn behave-puppet [world puppet]
  (-> world
      (behave-eat puppet)
      (behave-hunger puppet)))

(defn behave-puppets-map [world puppets]
  (let [[cur-id cur-puppet] (first puppets)]
    (if cur-id
      (behave-puppets-map
       (behave-puppet world cur-puppet)
       (rest puppets))
      world)))

(defn behave-puppets [world]
  (behave-puppets-map world (vec (:puppets world))))

(defn gen-puppets [world]
  (let [mrem (rem (:mstate world) puppets-gen-period)]
    (if (zero? mrem)
      (let [cells (:cells world)
            new-puppets-map (into {} (map gen-puppet-on-cell cells))]
        (update-in world [:puppets] merge new-puppets-map))
      world)))

(defn update-resource-cell [world cell]
  (let [resources-map (:resources cell)]
    (assoc-in world [:cells [(:x cell) (:y cell)] :resources]
              (into
               {}
               (for [[type resource] resources-map
                     :when (not= resource nil)]
                 [type (increment-resource resource)])))))

(defn update-resources-map [world cells]
  (let [[cur-loc cur-cell] (first cells)]
    (if cur-loc
      (update-resources-map
       (update-resource-cell world cur-cell)
       (rest cells))
      world)))

(defn update-resources [world]
  (let [mrem (rem (:mstate world) resource-gen-period)]
    (if (zero? mrem)
      (update-resources-map world (vec (:cells world)))
      world)))

(defn mstate-increment [world]
  (update-in world [:mstate] inc))

(defn recalc-world [world]
  (-> world
      behave-puppets
      update-resources
      gen-puppets
      mstate-increment))
