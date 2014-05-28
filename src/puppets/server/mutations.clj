(ns puppets.server.mutations
  (:use
   [puppets.server
    constants
    model]))

(defn add-building [user-id subtype loc village-loc]
  (let [world @world]
    (if (= user-id (-> (cell-at world village-loc) :village :user-id))
      (let [b-desc (get $buildings-description$ subtype)]
        (if b-desc
          (let [building (create-building! :building subtype 10 loc village-loc)]
            {:building
             {:id (:id building)
              :loc (:loc building)
              :type (:type building)
              :subtype (:subtype building)
              :puppet-ids (:puppet-ids building)}})
          (throw (Exception. "Unknown building."))))
      (throw (Exception. "Can't build for village not owned by you.")))))

(defn add-war-order [user-id village-loc target-loc forces]
  (let [world @world]
    (if (= user-id (-> (cell-at world target-loc) :village :user-id))
      (throw (Exception. "Can't attack own village."))
      (if (= user-id (-> (cell-at world village-loc) :village :user-id))
        (let [war-order (place-war-order! village-loc target-loc forces)]
          {:war-order
           {:target-loc (:target-loc war-order)
            :forces (:forces war-order)}})
        (throw (Exception. "Can't attack from village not owned by you."))))))

(defn add-training-order [user-id building-id warrior quantity-string]
  (if (not= "" quantity-string)
    (let [world @world
          quantity (Integer/parseInt quantity-string)
          building (-> world :buildings (get building-id))]
      (if (= user-id (-> (cell-at world (:village-loc building)) :village :user-id))
        (if (contains? $army-description$ warrior)
          (let [training-order (place-training-order! building-id warrior quantity)]
            {:id (:id training-order)
             :warrior (:warrior training-order)
             :quantity (:quantity training-order)})
          (throw (Exception. "Unknown warrior type.")))
        (throw (Exception. "Can't give order in building not owned by you."))))
    (throw (Exception. (str "Error quantity = '" quantity-string "'")))))

(defn add-production-order [user-id building-id product quantity-string]
  (if (not= "" quantity-string)
    (let [world @world
          quantity (Integer/parseInt quantity-string)
          building (-> world :buildings (get building-id))]
      (if (= user-id (-> (cell-at world (:village-loc building)) :village :user-id))
        (if (contains? (apply merge (vals $buildings-production$)) product)
          (let [production-order (place-production-order! building-id product quantity)]
            {:id (:id production-order)
             :product (:product production-order)
             :quantity (:quantity production-order)})
          (throw (Exception. "Unknown product type.")))
        (throw (Exception. "Can't give order in building not owned by you."))))
    (throw (Exception. (str "Error quantity = '" quantity-string "'")))))
