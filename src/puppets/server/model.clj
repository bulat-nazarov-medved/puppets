(ns puppets.server.model
  (:use
   [puppets.server constants])
  (:require
   [crypto.password.scrypt :as psw])
  (:import
   [java.lang Math]))

(defrecord World [mstate users cells puppets buildings])

(defrecord User [id name password active])

(defrecord Cell [loc resources village])

(defrecord Village [name user-id storage war-orders])

;;; resources - :cpu, :bytecode, :ram
(defrecord Resource [type cur-val max-val step-diff])

;;; war-state - nil, :attack, :return
(defrecord Puppet [id name type subtype loc hunger state village-loc
                   busyness war-target war-state carry health])

;;; types - :resource
;;; subtypes - :cpufreqd, :proguard, :ram.booster
(defrecord Building [id type subtype capacity loc village-loc puppet-ids])

(defrecord ProductionOrder [id puppet-takts product quantity])

(defrecord TrainingOrder [id puppet-takts warrior quantity])

(defrecord WarOrder [id target-loc forces])

(defrecord DBkey [key value])

(def dbkeys (atom {"general_pool" (atom 0)}))

(defn genid [key]
  (let [key-atom (get @dbkeys key)]
    (locking key-atom
      (let [next-id @key-atom]
        (swap! key-atom inc)
        next-id))))

(defn general-pool []
  (genid "general_pool"))

(defn create-world []
  (map->World {:mstate 0
               :users {}
               :cells {}
               :puppets {}
               :buildings {}}))

(defn distance [loc-from loc-to]
  (->> (map - loc-from loc-to)
       (map #(* % %))
       (apply +)
       Math/sqrt))

(defn cell-at [world loc]
  (get (:cells world) loc))

(defn gen-resource-always [type p]
  (let [max-val (int (* p 8000))
        cur-val (int (/ max-val 2))
        step-diff (int (/ max-val 100))]
    (map->Resource {:type type
                    :cur-val cur-val
                    :max-val max-val
                    :step-diff step-diff})))

(defn gen-resource [type p]
  [type (when (< (rand) p)
          (gen-resource-always type p))])

(defn increment-resource [{:keys [cur-val max-val step-diff] :as resource}]
  (let [new-val-raw (+ cur-val step-diff)
        new-val (if (> new-val-raw max-val)
                  max-val
                  new-val-raw)]
    (assoc resource :cur-val new-val)))

(defn gen-puppet-on-cell [[loc cell]]
  (let [id (general-pool)]
    [id (map->Puppet {:id id
                      :name (gensym "Puppet")
                      :loc loc
                      :type :general
                      :subtype :general
                      :hunger 0
                      :state :none
                      :village-loc nil
                      :busyness nil
                      :health 100})]))

(defn create-user [id name password active]
  (map->User {:id id
              :name name
              :password password
              :active active}))

(defn create-village [loc user-id]
  (map->Village {:name (gensym "Village")
                 :user-id user-id
                 :storage {:cpu 400000
                           :bytecode 200
                           :ram 100
                           :null-pointer-exception 100
                           :stack-overflow-exception 0}
                 :war-orders (sorted-map)}))

(defn place-village [world loc village]
  (-> world
      (assoc-in [:cells loc :village] village)
      (assoc-in [:cells loc :resources]
                {:cpu (gen-resource-always :cpu 1)
                 :bytecode (gen-resource-always
                            :bytecode 0.37)
                 :ram nil})))

(defn create-building [type subtype capacity loc village-loc]
  (let [id (general-pool)]
    (map->Building {:id id
                    :type type
                    :subtype subtype
                    :capacity capacity
                    :loc loc
                    :village-loc village-loc
                    :puppet-ids #{}
                    :production-orders (sorted-map)
                    :training-orders (sorted-map)})))

(defn place-building [world building]
  (assoc-in world [:buildings (:id building)] building))

(def world (agent (create-world)))

(defn resource-buildings [world]
  (filter
   (fn [building]
     (= :resource (:type building)))
   (vals (:buildings world))))

(defn production-buildings [world]
  (filter
   (fn [building]
     (= :production (:type building)))
   (vals (:buildings world))))

(defn building-buildings [world]
  (filter
   (fn [building]
     (= :building (:type building)))
   (vals (:buildings world))))

(defn military-buildings [world]
  (filter
   (fn [building]
     (= :military (:type building)))
   (vals (:buildings world))))

(defn village-cells [world]
  (filter
   (fn [cell]
     (if (:village cell)
       true
       false))
   (vals (:cells world))))

(defn village-puppets [world village-loc]
  (filter
   (fn [puppet]
     (= village-loc (:village-loc puppet)))
   (vals (:puppets world))))

(defn village-buildings [world village-loc]
  (filterv
   (fn [building]
     (= village-loc (:loc building)))
   (vals (:buildings world))))

(defn village-war-orders [world village-loc]
  (vals (-> world :cells (get village-loc) :village :war-orders)))

(defn village-forces [world village-loc]
  (into
   {}
   (for [force-subtype (keys $army-description$)]
     [force-subtype (into
                     #{}
                     (map
                      (fn [puppet]
                        (:id puppet))
                      (filter
                          (fn [puppet]
                            (and (= village-loc (:loc puppet))
                                 (= village-loc (:village-loc puppet))
                                 (= :military (:type puppet))
                                 (= force-subtype (:subtype puppet))))
                          (vals (:puppets world)))))])))

(defn non-trained-puppet-ids [world building]
  (let [puppet-ids (:puppet-ids building)]
    (into #{}
          (keep
           identity
           (map
            (fn [id]
              (let [puppet (-> world :puppets (get id))]
                (when (not= :military (:type puppet))
                  (:id puppet))))
            puppet-ids)))))

(defn get-current-order [building]
  (first (vals (:production-orders building))))

(defn get-current-training [building]
  (first (vals (:training-orders building))))

(defn extracted-resource [subtype]
  (case subtype
    :cpufreqd :cpu
    :proguard :bytecode
    :ram.booster :ram))

(defn create-production-order [product quantity]
  (map->ProductionOrder {:id (general-pool)
                         :puppet-takts 0
                         :product product
                         :quantity quantity}))

(defn place-production-order [world building-id production-order]
  (update-in world [:buildings building-id :production-orders]
             assoc (:id production-order) production-order))

(defn create-training-order [warrior quantity]
  (map->TrainingOrder {:id (general-pool)
                       :puppet-takts 0
                       :warrior warrior
                       :quantity quantity}))

(defn place-training-order [world building-id training-order]
  (update-in world [:buildings building-id :training-orders]
             assoc (:id training-order) training-order))

(defn create-war-order [target-loc forces]
  (map->WarOrder {:id (general-pool)
                  :target-loc target-loc
                  :forces forces}))

(defn place-war-order [world village-loc war-order]
  (update-in world [:cells village-loc :village :war-orders]
             assoc (:id war-order) war-order))

(defn user-village-cells [world user-id]
  (filter
   (fn [cell]
     (and (-> cell :village)
          (= user-id (-> cell :village :user-id))))
   (vals (:cells world))))

(defn user-check-village [world user-id]
  (let [village-cells (user-village-cells world user-id)]
    (if (= 0 (count village-cells))
      (let [free-cell (first
                       (shuffle
                        (filter
                         (fn [cell]
                           (not (-> cell :village)))
                         (vals (:cells world)))))
            new-village (create-village (:loc free-cell) user-id)
            new-building (create-building :building :cpufreqd 10
                                          (:loc free-cell) (:loc free-cell))
            new-building2 (create-building :production :throws 2
                                          (:loc free-cell) (:loc free-cell))
            new-building3 (create-building :military :class 10
                                          (:loc free-cell) (:loc free-cell))]
        (-> world
            (place-village (:loc free-cell) new-village)
            (place-building new-building)
            (place-building new-building2)
            (place-building new-building3)))
      world)))

(defn clear-world! []
  (send world (constantly (create-world))))

(defn create-cell! [loc]
  (let [cell-resources (into {}
                             (map gen-resource
                                  [:cpu :bytecode :ram]
                                  [1 0.37 0.13]))
        new-cell (map->Cell {:loc loc
                             :resources cell-resources})]
    (send world assoc-in [:cells loc] new-cell)))

(defn create-user! [id name password active]
  (let [new-user (create-user id name password active)]
    (send world update-in [:users] assoc id new-user)
    new-user))

(defn activate-user! [id]
  (send world assoc-in [:users id :active] true))

(defn create-village! [loc user-id]
  (let [new-village (create-village loc user-id)]
    (send world place-village loc new-village)
    new-village))

(defn create-building! [type subtype capacity loc village-loc]
  (let [new-building (create-building type subtype capacity loc village-loc)]
    (send world place-building new-building)
    new-building))

(defn place-production-order! [building-id product quantity]
  (let [new-production-order (create-production-order product quantity)]
    (send world place-production-order building-id new-production-order)
    new-production-order))

(defn place-training-order! [building-id warrior quantity]
  (let [new-training-order (create-training-order warrior quantity)]
    (send world place-training-order building-id new-training-order)
    new-training-order))

(defn place-war-order! [village-loc target-loc forces]
  (let [new-war-order (create-war-order target-loc forces)]
    (send world place-war-order village-loc new-war-order)
    new-war-order))

(defn search-user! [name password]
  (let [found-users (filterv
                     (fn [user]
                       (and (= name (:name user))
                            (psw/check password (:password user))))
                     (vals (:users @world)))]
    (if (= 1 (count found-users))
      (first found-users)
      nil)))

(defn user-check-village! [user-id]
  (send world user-check-village user-id))
