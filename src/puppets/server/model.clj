(ns puppets.server.model
  (:import
   [java.lang Math]))

(defrecord World [mstate users cells puppets buildings])

(defrecord User [id name])

(defrecord Cell [loc resources village])

(defrecord Village [name user-id storage])

;;; resources - :cpu, :bytecode, :ram
(defrecord Resource [type cur-val max-val step-diff])

(defrecord Puppet [id name type subtype loc hunger state village-loc busyness])

;;; types - :resource
;;; subtypes - :cpufreqd, :proguard, :ram.booster
(defrecord Building [id type subtype capacity loc village-loc puppet-ids])

(defrecord ProductionOrder [id puppet-takts product quantity])

(defrecord TrainingOrder [id puppet-takts warrior quantity])

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
                      :busyness nil})]))

(defn create-user [name]
  (let [id (general-pool)]
    [id (map->User {:id id
                    :name name})]))

(defn create-village [loc user-id]
  (map->Village {:name (gensym "Village")
                 :user-id user-id
                 :storage {:cpu 400
                           :bytecode 200
                           :ram 100
                           :null-pointer-exception 0
                           :stack-overflow-exception 0}}))

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

(defn create-user! [name]
  (let [new-user (create-user name)]
    (send world update-in [:users] merge new-user)
    (second new-user)))

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
