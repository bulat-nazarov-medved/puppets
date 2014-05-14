(ns puppets.server.behavior
  (:use
   [puppets.server constants model])
  (:require
   [clojure.math.numeric-tower :as math]))

(defn cells-puppet-area [world pupet]
  )

(defn puppet-want-to-eat [world puppet]
  (assoc-in world [:puppets (:id puppet) :state] :want-to-eat))

(defn puppet-die [world puppet]
  (-> world
      (update-in [:puppets] dissoc (:id puppet))
      (update-in [:buildings] #(for [building %]
                                 (if (= (:id puppet) (:puppet-id building))
                                   (assoc building :puppet-id nil)
                                   building)))))

(defn behave-hunger [world puppet]
  (let [hunger (:hunger puppet)]
    (cond
     (> hunger hunger-to-death) (puppet-die world puppet)
     (> hunger hunger-to-eat) (puppet-want-to-eat world puppet)
     :else world)))

(defn eat-from-village [world puppet village-loc]
  (let [cpu-res (-> world
                    :cells
                    (get village-loc)
                    :village
                    :storage
                    :cpu)]
    (if (>= cpu-res _puppet-eat-at-once_)
      (-> world
          (assoc-in [:puppets (:id puppet) :hunger] 0)
          (update-in [:cells village-loc :village :storage :cpu]
                     - _puppet-eat-at-once_))
      world)))

(defn behave-eat [world puppet]
  (if (= :want-to-eat (:state puppet))
    (let [village-loc (:village-loc puppet)]
      (if village-loc
        (eat-from-village world puppet village-loc)
        world))
    world))

(defn became-hunger [world puppet]
  (update-in world [:puppets (:id puppet) :hunger] inc))

(defn cell-villages-near [world puppet]
  (vec
   (vals
    (filter
     (fn [[loc cell]]
       (and (:village cell)
            (<= (distance loc (:loc puppet)) _puppet-sight-range_)))
     (:cells world)))))

(defn cell-village-population [world cell-village]
  (count (filter (fn [puppet]
                   (= (:village-loc puppet)
                      (:loc cell-village)))
                 (:puppets world))))

(defn cell-villages-population [world cell-villages]
  (into {}
        (for [cv cell-villages]
          [(:loc cv) (cell-village-population world cv)])))

(defn choose-best-cell-village [world villages]
  (let [cv-pop (cell-villages-population world villages)]
    (reduce (fn [cv1 cv2]
              (cond
               (and (nil? cv1) (nil? cv2)) nil
               (and (nil? cv1) (not (nil? cv2))) cv2
               (and (not (nil? cv1)) (nil? cv2)) cv1
               :else (let [cv1-pop (get cv-pop (:loc cv1))
                           cv2-pop (get cv-pop (:loc cv2))]
                       (if (> cv1-pop cv2-pop)
                         cv1
                         cv2))))
            nil
            villages)))

(defn max-abs [& a]
  (reduce
   (fn [a1 a2]
     (if (> (math/abs a1) (math/abs a2))
       a1
       a2))
   (first a)
   (rest a)))

(defn puppet-move-to [world puppet loc]
  (let [puppet-loc (:loc puppet)
        locs-diff (vec (map - loc puppet-loc))
        max-cv (apply max-abs locs-diff)
        max-ci (.indexOf locs-diff max-cv)
        c-op (cond
              (pos? max-cv) inc
              (neg? max-cv) dec
              :else identity)]
    (if (not= max-cv 0)
      (update-in world [:puppets (:id puppet) :loc max-ci] c-op)
      world)))

(defn find-village [world puppet]
  (if-not (:village-loc puppet)
    (let [cell-villages-near (cell-villages-near world puppet)
          best-village-to-live (choose-best-cell-village
                                world cell-villages-near)]
      (if best-village-to-live
        (puppet-move-to world puppet (:loc best-village-to-live))
        world))
    world))

(defn became-citizen [world puppet]
  (if-not (:village-loc puppet)
    (let [cell-villages-near (cell-villages-near world puppet)
          best-village-to-live (choose-best-cell-village
                                world cell-villages-near)]
      (if (= (:loc best-village-to-live) (:loc puppet))
        (assoc-in world [:puppets (:id puppet) :village-loc] (:loc puppet))
        world))
    world))

(defn works-for-puppet [world puppet]
  (filter (fn [building]
            (and (= (:village-loc building) (:village-loc puppet))
                 (nil? (:puppet-id building))))
          (vals (:buildings world))))

(defn nearest-object [loc objects]
  (reduce
   (fn [o1 o2]
     (< (distance loc (:loc o1))
        (distance loc (:loc o2))))
   (first objects)
   (rest objects)))

(defn puppet-begin-work [world puppet building]
  (-> world
      (assoc-in [:buildings (:id building) :puppet-id] (:id puppet))
      (assoc-in [:puppets (:id puppet) :busyness] true)))

(defn loc-at? [loc1 loc2]
  (= loc1 loc2))

(defn puppet-find-busyness [world puppet]
  (if (and (:village-loc puppet)
           (not (:busyness puppet)))
    (let [available-works (works-for-puppet world puppet)
          nearest-work (nearest-object (:loc puppet) available-works)]
      (if nearest-work
        (if (loc-at? (:loc puppet) (:loc nearest-work))
          (puppet-begin-work world puppet nearest-work)
          (puppet-move-to world puppet (:loc nearest-work)))
        world))
    world))

(defn behave-puppet [world puppet]
  (-> world
      (behave-eat puppet)
      (became-hunger puppet)
      (find-village puppet)
      (became-citizen puppet)
      (puppet-find-busyness puppet)
      (puppet-find-busyness puppet)
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
    (assoc-in world [:cells (:loc cell) :resources]
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

(defn extract-resource [world building]
  world)

(defn extract-resources [world]
  (reduce
   (fn [world' building]
     (extract-resource world' building))
   world
   (:buildings world)))

(defn recalc-world [world]
  (-> world
      behave-puppets
      update-resources
      extract-resources
      gen-puppets
      mstate-increment))
