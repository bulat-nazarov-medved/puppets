(ns puppets.server.model)

(defrecord World [mstate users cells puppets])

(defrecord User [id])

(defrecord Cell [x y resources])

;;; resources - :cpu, :bytecode, :ram
(defrecord Resource [type cur-val max-val step-diff])

(defrecord Puppet [id name x y hunger state])

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

(defn gen-resource [type p]
  [type (when (< (rand) p)
          (let [max-val (int (* p 8000))
                cur-val (int (/ max-val 2))
                step-diff (int (/ max-val 1000))]
            (map->Resource {:type type
                            :cur-val cur-val
                            :max-val max-val
                            :step-diff step-diff})))])

(defn increment-resource [{:keys [cur-val max-val step-diff] :as resource}]
  (let [new-val-raw (+ cur-val step-diff)
        new-val (if (> new-val-raw max-val)
                  max-val
                  new-val-raw)]
    (assoc resource :cur-val new-val)))

(defn gen-puppet-on-cell [[[x y] cell]]
  (let [id (general-pool)]
    [id (map->Puppet {:id id
                      :name (gensym "Puppet")
                      :x x
                      :y y
                      :hunger 0
                      :state :none})]))

(def world (agent (create-world)))

(defn clear-world! []
  (send world (constantly (create-world))))

(defn create-cell! [[x y]]
  (let [cell-resources (into {}
                             (map gen-resource
                                  [:cpu :bytecode :ram]
                                  [1 0.37 0.13]))
        new-cell (map->Cell {:x x :y y
                             :resources cell-resources})]
    (send world assoc-in [:cells [x y]] new-cell)))

