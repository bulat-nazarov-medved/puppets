(ns puppets.server.model)

(defrecord World [mstate users cells puppets])

(defrecord User [id name])

(defrecord Cell [loc resources village])

(defrecord Village [name user-id storage])

;;; resources - :cpu, :bytecode, :ram
(defrecord Resource [type cur-val max-val step-diff])

(defrecord Puppet [id name loc hunger state village-loc])

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
               :puppets {}}))

(defn cell-at [world loc]
  (get (:cells world) loc))

(defn gen-resource-always [type p]
  (let [max-val (int (* p 8000))
        cur-val (int (/ max-val 2))
        step-diff (int (/ max-val 1000))]
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
                      :hunger 0
                      :state :none
                      :village-loc nil})]))

(defn create-user [name]
  (let [id (general-pool)]
    [id (map->User {:id id
                    :name name})]))

(defn create-village [loc user-id]
  (map->Village {:name (gensym "Village")
                 :user-id user-id
                 :storage {:cpu 400
                           :bytecode 200
                           :ram 100}}))

(defn place-village [world loc village]
  (-> world
      (assoc-in [:cells loc :village] village)
      (assoc-in [:cells loc :resources]
                {:cpu (gen-resource-always :cpu 1)
                 :bytecode (gen-resource-always
                            :bytecode 0.37)
                 :ram nil})))

(def world (agent (create-world)))

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
