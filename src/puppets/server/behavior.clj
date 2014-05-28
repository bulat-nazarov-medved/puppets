(ns puppets.server.behavior
  (:use
   [puppets.server constants model])
  (:require
   [clojure.math.numeric-tower :as math]))

(defn puppet-die [world puppet]
  (if (= :dead (:state puppet))
    (-> (if (:busyness puppet)
          (update-in world [:buildings (:busyness puppet) :puppet-ids]
                     disj (:id puppet))
          world)
        (update-in [:puppets] dissoc (:id puppet)))
    world))

(defn behave-hunger [world puppet]
  (let [hunger (:hunger puppet)]
    (cond
     (> hunger hunger-to-death) (assoc-in world [:puppets (:id puppet) :state] :dead)
     (> hunger hunger-to-eat) (assoc-in world [:puppets (:id puppet) :state] :want-to-eat)
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
          (assoc-in [:puppets (:id puppet) :state] :none)
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
                 (< (count (:puppet-ids building)) (:capacity building))))
          (vals (:buildings world))))

(defn nearest-object [loc objects]
  (reduce
   (fn [o1 o2]
     (if (< (distance loc (:loc o1))
            (distance loc (:loc o2)))
       o1
       o2))
   (first objects)
   (rest objects)))

(defn puppet-begin-work [world puppet building]
  (-> world
      (update-in [:buildings (:id building) :puppet-ids] conj (:id puppet))
      (assoc-in [:puppets (:id puppet) :busyness] (:id building))))

(defn loc-at? [loc1 loc2]
  (= loc1 loc2))

(defn puppet-find-busyness [world puppet']
  (let [puppet (-> world :puppets (get (:id puppet')))]
    (if (and (:village-loc puppet)
             (not (:busyness puppet)))
      (let [available-works (works-for-puppet world puppet)
            nearest-work (nearest-object (:loc puppet) available-works)]
        (if nearest-work
          (if (loc-at? (:loc puppet) (:loc nearest-work))
            (puppet-begin-work world puppet nearest-work)
            (puppet-move-to world puppet (:loc nearest-work)))
          world))
      world)))

(defn military-puppet-go-target [world puppet]
  (let [war-target-loc (:war-target puppet)]
    (if (and war-target-loc
             (= :military (:type puppet))
             (not= war-target-loc (:loc puppet)))
      (puppet-move-to world puppet war-target-loc)
      world)))

(defn military-puppet-fight [world att-puppet def-puppets]
  (let [def-puppet (first (shuffle
                             (for [[_ puppet-ids] def-puppets
                                   puppet-id puppet-ids]
                               (-> world :puppets (get puppet-id)))))
        def-subtype (:subtype def-puppet)
        def-health (:health def-puppet)
        {[ld hd] :defence} (get $army-description$ def-subtype)
        def-value (+ ld (math/round (rand (- hd ld))))
        att-subtype (:subtype att-puppet)
        att-health (:health att-puppet)
        {[la ha] :attack} (get $army-description$ att-subtype)
        att-value (+ la (math/round (rand (- ha la))))]
    (if (> att-health def-value)
      (-> (if (> def-health att-value)
            (update-in world [:puppets (:id def-puppet) :health] - att-value)
            (assoc-in world [:puppets (:id def-puppet) :state] :dead))
          (update-in [:puppets (:id att-puppet) :health] - def-value))
      (assoc-in world [:puppets (:id att-puppet) :state] :dead))))

(defn military-puppet-sack-village [world puppet]
  (let [subtype (:subtype puppet)
        subtype-desc (get $army-description$ subtype)
        can-carry (:carry subtype-desc)
        village-res (into
                     {} (shuffle
                         (filterv
                          (fn [[_ q]]
                            (> q 0))
                          (-> (cell-at world (:loc puppet))
                              :village :storage))))
        [rtype rquantity] (first village-res)]
    (if rtype
      (let [rob-count (if (> rquantity can-carry)
                        can-carry
                        rquantity)]
        (-> world
            (update-in [:cells (:loc puppet) :village :storage rtype]
                       - rob-count)
            (assoc-in [:puppets (:id puppet) :carry] {rtype rob-count})
            (assoc-in [:puppets (:id puppet) :war-state] :return)
            (assoc-in [:puppets (:id puppet) :war-target] nil)))
      world)))

(defn military-puppet-attack [world puppet]
  (let [war-target-loc (:war-target puppet)]
    (if (and war-target-loc
             (= :military (:type puppet))
             (= war-target-loc (:loc puppet)))
      (let [enemy-puppets (village-forces world war-target-loc)
            enemy-count (apply + (map count (vals enemy-puppets)))]
        (if (> enemy-count 0)
          (military-puppet-fight world puppet enemy-puppets)
          (military-puppet-sack-village world puppet)))
      world)))

(defn military-puppet-unload [world puppet]
  (-> world
      (update-in [:cells (:village-loc puppet) :village :storage]
                 (fn [resources]
                   (merge-with + resources (:carry puppet))))
      (assoc-in [:puppets (:id puppet) :carry] nil)
      (assoc-in [:puppets (:id puppet) :war-state] nil)))

(defn military-puppet-return [world puppet]
  (if (= :return (:war-state puppet))
    (if (not= (:loc puppet) (:village-loc puppet))
      (puppet-move-to world puppet (:village-loc puppet))
      (military-puppet-unload world puppet))
    world))

(defn behave-puppet [world puppet]
  (-> world
      (behave-eat puppet)
      (became-hunger puppet)
      (find-village puppet)
      (became-citizen puppet)
      (puppet-find-busyness puppet)
      (puppet-find-busyness puppet)
      (military-puppet-go-target puppet)
      (military-puppet-attack puppet)
      (military-puppet-return puppet)
      (behave-hunger puppet)
      (puppet-die puppet)))

(defn behave-puppets [world]
  (reduce
   (fn [world' puppet]
     (behave-puppet world' puppet))
   world
   (vals (:puppets world))))

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
  (let [working-puppets (count (:puppet-ids building))]
    (if (> working-puppets 0)
      (let [extract-percentage (/ working-puppets (:capacity building))
            resource (extracted-resource (:subtype building))
            canonical-value (get $extract-resources-value$ resource)
            probable-value (int (* canonical-value extract-percentage))
            available (-> world :cells (get (:loc building)) :resources
                          resource :cur-val)
            actual-value (when available
                           (if (> probable-value available)
                             available probable-value))]
        (if available
          (-> world
              (update-in [:cells (:loc building) :resources resource :cur-val]
                         - actual-value)
              (update-in [:cells (:village-loc building) :village :storage resource]
                         + actual-value))
          world))
      world)))

(defn extract-resources [world]
  (reduce
   (fn [world' building]
     (extract-resource world' building))
   world
   (resource-buildings world)))

(defn progress-order [world building-id order-id quantity takts]
  (-> world
      (update-in [:buildings building-id :production-orders order-id :quantity]
                 - quantity)
      (assoc-in [:buildings building-id :production-orders order-id :puppet-takts]
                 takts)))

(defn complete-order [world building-id order-id]
  (update-in world [:buildings building-id :production-orders] dissoc order-id))

(defn move-to-storage [world village-loc product quantity]
  (update-in world [:cells village-loc :village :storage product]
             + quantity))

(defn produce-artifact [world building]
  (let [village-loc (:village-loc building)
        busy-puppets (count (:puppet-ids building))
        production-orders (count (:production-orders building))]
    (if (and (> busy-puppets 0)
             (> production-orders 0))
      (let [{:keys [id puppet-takts product quantity]} (get-current-order building)
            building-description (get $buildings-production$ (:subtype building))
            {p-quantity :quantity p-puppet-takts :puppet-takts needs :needs}
            (get building-description product)
            village-res (-> (cell-at world village-loc) :village :storage)
            needs-for-puppets (into {}
                                    (for [[res val] needs]
                                      [res (* val busy-puppets)]))
            res-after-takt (merge-with - village-res needs-for-puppets)
            res-enough? (if (< (apply min (vals res-after-takt)) 0) false true)
            new-takts (+ puppet-takts busy-puppets)
            world-with-production (if res-enough?
                                    (-> world
                                        (assoc-in [:cells village-loc
                                                   :village :storage]
                                                  res-after-takt)
                                        (assoc-in [:buildings (:id building)
                                                   :production-orders
                                                   id :puppet-takts]
                                                   new-takts))
                                    world)]
        (if (and res-enough?
                 (>= new-takts p-puppet-takts))
          (let [world-with-move-to-storage (move-to-storage world village-loc
                                                            product p-quantity)]
            (if (> (- quantity p-quantity) 0)
              (progress-order world-with-move-to-storage (:id building)
                              id p-quantity (- new-takts p-puppet-takts))
              (complete-order world-with-move-to-storage (:id building) id)))
          world-with-production))
      world)))

(defn produce-artifacts [world]
  (reduce
   (fn [world' building]
     (produce-artifact world' building))
   world
   (production-buildings world)))

(defn perform-building [world building]
  (let [village-loc (:village-loc building)
        busy-puppet-ids (:puppet-ids building)
        busy-puppets (count busy-puppet-ids)]
    (if (> busy-puppets 0)
      (let [cur-takts (or (:puppet-takts building) 0)
            subtype (:subtype building)
            b-desc (get $buildings-description$ subtype)
            {:keys [type capacity needs building-takts]} b-desc
            village-res (-> (cell-at world village-loc) :village :storage)
            needs-for-puppets (into {}
                                    (for [[res val] needs]
                                      [res (* val busy-puppets)]))
            res-after-takt (merge-with - village-res needs-for-puppets)
            res-enough? (if (< (apply min (vals res-after-takt)) 0) false true)
            new-takts (+ cur-takts busy-puppets)]
        (if res-enough?
          (if (>= new-takts building-takts)
            (-> world
                (assoc-in [:buildings (:id building)]
                          (assoc (create-building type subtype capacity
                                                  (:loc building) village-loc)
                            :id (:id building)))
                (update-in [:puppets] (fn [puppets]
                                        (into {}
                                              (for [[id puppet] puppets]
                                                [id (if (= (:id building)
                                                           (:busyness puppet))
                                                      (assoc puppet :busyness nil)
                                                      puppet)])))))
            (assoc-in world [:buildings (:id building) :puppet-takts] new-takts))
          world))
      world)))

(defn perform-buildings [world]
  (reduce
   (fn [world' building]
     (perform-building world' building))
   world
   (building-buildings world)))

(defn train-warrior [world building]
  (let [cur-training (get-current-training building)]
    (if cur-training
      (let [non-trained-puppet-ids (non-trained-puppet-ids world building)]
        (if (> (count non-trained-puppet-ids) 0)
          (let [village-loc (:village-loc building)
                {t-id :id t-takts :puppet-takts
                 warrior :warrior t-quantity :quantity} cur-training
                {w-takts :takts w-needs :needs} (get $army-description$ warrior)
                new-takts (inc t-takts)]
            (if (>= new-takts w-takts)
              (let [village-res (-> (cell-at world village-loc) :village :storage)
                    res-after-takt (merge-with - village-res w-needs)
                    res-enough? (>= (apply min (vals res-after-takt)) 0)]
                (if res-enough?
                  (let [new-quantity (dec t-quantity)
                        puppet-id (first non-trained-puppet-ids)]
                    (-> (if (= 0 new-quantity)
                          (-> world
                              (update-in [:buildings (:id building)
                                          :training-orders]
                                         dissoc t-id))
                          (-> world
                              (assoc-in [:buildings (:id building)
                                         :training-orders t-id :quantity] new-quantity)
                              (assoc-in [:buildings (:id building)
                                         :training-orders t-id :puppet-takts] 0)))
                        (assoc-in [:puppets puppet-id :type] :military)
                        (assoc-in [:puppets puppet-id :subtype] warrior)
                        (assoc-in [:cells village-loc :village :storage] res-after-takt)))
                  world))
              (assoc-in world [:buildings (:id building)
                               :training-orders t-id :puppet-takts] new-takts)))
          world))
      world)))

(defn train-warriors [world]
  (reduce
   (fn [world' building]
     (train-warrior world' building))
   world
   (military-buildings world)))

(defn send-puppet-to-war [world target puppet-id]
  (-> world
      (assoc-in [:puppets puppet-id :war-target] target)
      (assoc-in [:puppets puppet-id :war-state] :attack)))

(defn send-forces-from-village [world target forces]
  (reduce
   (fn [world' puppet-id]
     (send-puppet-to-war world' target puppet-id))
   world
   (vec
    (for [[_ puppet-ids] forces
          puppet-id puppet-ids]
      puppet-id))))

(defn go-war-from-village [world cell]
  (let [current-war-order (first (-> cell :village :war-orders vals))]
    (if current-war-order
      (let [need-forces (:forces current-war-order)
            current-forces (village-forces world (:loc cell))
            current-forces-count (into
                                  {}
                                  (map
                                   (fn [[subtype puppet-ids]]
                                     [subtype (count puppet-ids)])
                                   current-forces))
            forces-after-give-order (merge-with - current-forces-count
                                                (:forces current-war-order))
            forces-enough? (>= (apply min (vals forces-after-give-order)) 0)]
        (if forces-enough?
          (-> (send-forces-from-village world (:target-loc current-war-order)
                                        (merge-with
                                         (fn [ids count]
                                           (into #{} (take count ids)))
                                         current-forces need-forces))
              (update-in [:cells (:loc cell) :village :war-orders]
                         dissoc (:id current-war-order)))
          world))
      world)))

(defn go-war [world]
  (reduce
   (fn [world' cell]
     (go-war-from-village world' cell))
   world
   (village-cells world)))

(defn recalc-world [world]
  (-> world
      behave-puppets
      update-resources
      extract-resources
      produce-artifacts
      perform-buildings
      train-warriors
      go-war
      gen-puppets
      mstate-increment))

(defn recalc-world! []
  (send world recalc-world))
