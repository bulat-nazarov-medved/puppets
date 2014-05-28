(ns puppets.server.constants)

(def dim 5)

(def hunger-to-eat 15)

(def hunger-to-death 25)

(def puppets-gen-period 25)

(def resource-gen-period 10)

(def _puppet-eat-at-once_ 10)

(def _puppet-sight-range_ 5)

(def $extract-resources-value$
  {:cpu 10
   :bytecode 4
   :ram 1})

(def $army-description$
  {:object {:takts 3
            :needs {:null-pointer-exception 1}
            :carry 10
            :attack [4 6]
            :defence [6 8]}
   :metaobject {:takts 6
                :needs {:stack-overflow-exception 1}
                :carry 5
                :attack [7 9]
                :defence [3 5]}})

(def $buildings-description$
  {:cpufreqd     {:type :resource
                  :description "cpufreqd"
                  :capacity 1
                  :building-capacity 5
                  :building-takts 200
                  :needs {:cpu 0
                          :bytecode 1
                          :ram 0}}
   :proguard    {:type :resource
                 :description "proguard"
                 :capacity 1
                 :building-capacity 5
                 :building-takts 200
                 :needs {:cpu 0
                         :bytecode 1
                         :ram 0}}
   :ram.booster {:type :resource
                 :description "RAM booster"
                 :capacity 1
                 :building-capacity 5
                 :building-takts 200
                 :needs {:cpu 0
                         :bytecode 1
                         :ram 0}}
   :throws      {:type :production
                 :description "Throws"
                 :capacity 2
                 :building-capacity 10
                 :building-takts 400
                 :needs {:cpu 0
                         :bytecode 1
                         :ram 0}}
   :class       {:type :military
                 :description "Class"
                 :capacity 10
                 :building-capacity 10
                 :building-takts 1000
                 :needs {:cpu 0
                         :bytecode 1
                         :ram 0}}})

(def $buildings-production$
  {:throws {:null-pointer-exception
            {:quantity 1
             :puppet-takts 6
             :needs {:cpu 1
                     :bytecode 6}}
            :stack-overflow-exception
            {:quantity 1
             :puppet-takts 12
             :needs {:cpu 1
                     :bytecode 2
                     :ram 2}}}})
