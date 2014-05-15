(ns puppets.server.constants)

(def dim 5)

(def hunger-to-eat 3)

(def hunger-to-death 5)

(def puppets-gen-period 7)

(def resource-gen-period 10)

(def _puppet-eat-at-once_ 10)

(def _puppet-sight-range_ 5)

(def $extract-resources-value$
  {:cpu 10
   :bytecode 4
   :ram 1})

(def $buildings-production$
  {:throws {:null-pointer-exception
            {:quantity 1
             :mstates 3
             :needs {:cpu 1
                     :bytecode 10}}
            :stack-overflow-exception
            {:quantity 1
             :mstates 6
             :needs {:cpu 1
                     :bytecode 4
                     :ram 5}}}})
