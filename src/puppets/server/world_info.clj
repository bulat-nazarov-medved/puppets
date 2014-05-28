(ns puppets.server.world-info
  (:require
   [puppets.server.model :as m]
   [noir.session :as session]))

(defn print-world []
  {:world @m/world})

(defn print-world-agent []
  (let [error (agent-error m/world)
        s (java.io.StringWriter.)
        p (java.io.PrintWriter. s)]
    (.printStackTrace error p)
    {:world s}))

(defn _village-info [world village-cell]
  (let [village (:village village-cell)]
    {:loc (:loc village-cell)
     :puppets (mapv
               (fn [puppet]
                 {:id (:id puppet)
                  :loc (:loc puppet)
                  :name (:name puppet)
                  :type (:type puppet)
                  :subtype (:subtype puppet)
                  :hunger (:hunger puppet)
                  :health (:health puppet)
                  :busy? (if (:busyness puppet) true false)})
               (m/village-puppets world (:loc village-cell)))
     :cell-resources (mapv
                      (fn [[type resource]]
                        {:type type
                         :cur-val (:cur-val resource)
                         :max-val (:max-val resource)})
                      (:resources village-cell))
     :buildings (mapv
                 (fn [building]
                   (merge
                    {:id (:id building)
                     :loc (:loc building)
                     :type (:type building)
                     :subtype (:subtype building)
                     :puppet-ids (:puppet-ids building)}
                    (if (= :military (:type building))
                      {:training-orders (mapv
                                         (fn [t-order]
                                           {:id (:id t-order)
                                            :warrior (:warrior t-order)
                                            :quantity (:quantity t-order)})
                                         (vals (:training-orders building)))}
                      {})
                    (if (= :production (:type building))
                      {:production-orders (mapv
                                           (fn [p-order]
                                             {:id (:id p-order)
                                              :product (:product p-order)
                                              :quantity (:quantity p-order)})
                                           (vals (:production-orders building)))}
                      {})))
                 (m/village-buildings world (:loc village-cell)))
     :storage (:storage village)
     :forces (m/village-forces world (:loc village-cell))
     :war-orders (mapv
                  (fn [war-order]
                    {:target-loc (:target-loc war-order)
                     :forces (:forces war-order)})
                  (m/village-war-orders world (:loc village-cell)))}))

(defn _user-villages [world user-id]
  (let [user-village-cells (m/user-village-cells world user-id)]
    {:villages (mapv
                (fn [cell]
                  [(:loc cell) (-> cell :village :name)])
                user-village-cells)}))

(defn village-info [village-loc]
  (let [user-id (session/get :user)
        current-world @m/world
        cur-village-cell (if village-loc
                           (let [cell (m/cell-at current-world village-loc)]
                             (if (and (:village cell)
                                      (= user-id (-> cell :village :user-id)))
                               cell
                               (throw (Exception. "not your village"))))
                           (first
                            (m/user-village-cells
                             current-world user-id)))]
    (merge
     (_village-info current-world cur-village-cell)
     (_user-villages current-world user-id))))
