(ns puppets.client.village
  (:require
   [ajax.core :as ajax]
   [enfocus.core :as ef]
   [enfocus.events :as ev])
  (:require-macros
   [enfocus.macros :as em]))

(em/deftemplate building-internals-view "/prototype/building.html"
  [building]
  "#buildingtype" (ef/content (str (:type building)))
  "#buildingsubtype" (ef/content (str (:subtype building)))
  "#puppetids" (ef/content (str (:puppet-ids building)))
  "#reloadvillage" (ev/listen :click #(go-to-village (:loc building)))
  "#addtrainingorder" (ev/listen :click #(add-training-order-ui building))
  "#addproductionorder" (ev/listen :click #(add-production-order-ui building)))

(em/defsnippet add-training-order-view "/prototype/building.html" ["#addtrainingorderform"]
  []
  )

(em/defsnippet add-production-order-view "/prototype/building.html" ["#addproductionorderform"]
  []
  )

(em/deftemplate village-view "/prototype/village.html" [village-info]
  "#addbuilding" (ev/listen :click #(add-building-ui village-info))
  "#addwarorder" (ev/listen :click #(add-war-order-ui village-info)))

(em/deftemplate add-building-view "/prototype/addbuilding.html" [village-info]
  )

(em/defsnippet select-option "/prototype/addbuilding.html"
  ["#addbuildingform select[name='buildingtype'] option"]
  [id name]
  "option" (ef/set-attr :value (str id))
  "option" (ef/content (str name)))

(em/deftemplate add-war-order-view "/prototype/addwarorder.html" [village-info]
  )

(em/defsnippet force-value "/prototype/addwarorder.html"
  ["#addwarorderform ul li:first-child"]
  [type]
  "span.forcetype" (ef/content (str type))
  "input" (ef/set-attr :name (name type)))

(em/defsnippet building-view "/prototype/village.html" [".buildingview"]
  [building]
  "li" (ef/remove-attr :style)
  "li a" (ef/content (str building))
  "li a" (ev/listen :click #(go-to-building building)))

(em/defsnippet resource-view "/prototype/village.html" [".resourceview"]
  [resource]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str resource)))

(em/defsnippet storage-item-view "/prototype/village.html" [".storageitemview"]
  [storage-item]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str storage-item)))

(em/defsnippet force-view "/prototype/village.html" [".forceview"]
  [type puppet-ids]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str type " " puppet-ids)))

(em/defsnippet war-order-view "/prototype/village.html" [".warorderview"]
  [war-order]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str war-order)))

(em/defsnippet puppet-view "/prototype/village.html" [".puppetview"]
  [puppet]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str puppet)))

(em/defsnippet village-item "/prototype/main.html" [".villageitem"]
  [loc name]
  "a[href]" (ef/content name)
  "a[href]" (ev/listen :click #(go-to-village loc)))

(em/defsnippet training-order-view "/prototype/building.html" [".trainingorderview"]
  [training-order]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str training-order)))

(em/defsnippet production-order-view "/prototype/building.html" [".productionorderview"]
  [production-order]
  "li" (ef/remove-attr :style)
  "li" (ef/content (str production-order)))

(defn form-finally2 [form]
  (ef/at form "input[type='submit']" (ef/remove-attr :disabled)))

(defn error-response [response]
  (ef/at "#errorresponse" (ef/content (str "error: " response))))

(defn add-building-complete [village-info response]
  (if (:success response)
    (go-to-village (:loc village-info))
    (error-response response)))

(defn send-add-building [village-info form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit'" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/add-building"
              {:handler (partial add-building-complete village-info)
               :error-handler error-response
               :finally (partial form-finally2 form)
               :format :raw
               :params (merge {:loc (str (:loc village-info))
                               :village-loc (str (:loc village-info))}
                              form-values)})))

(defn building-types-received [village-info response]
  (ef/at "#content" (ef/content (add-building-view village-info)))
  (ef/at "#addbuildingform select[name='buildingtype']" (ef/content ""))
  (ef/at "#addbuildingform select[name='buildingtype']"
         (ef/content
          (map
           (fn [[type name]]
             (select-option type name))
           (:buildings response))))
  (ef/at "#addbuildingform"
         (ev/listen :submit #(do (.preventDefault %)
                                 (send-add-building
                                  village-info
                                  (.-currentTarget %)))))
  (ef/at "#addbuildingform .cancel" (ev/listen :click #(go-to-village (:loc vilalge-info)))))

(defn add-building-ui [village-info]
  (ajax/GET "/api/building-types"
            {:handler (partial building-types-received village-info)}))

(defn add-war-order-complete [village-info response]
  (if (:success response)
    (go-to-village (:loc village-info))
    (error-response response)))

(defn send-add-war-order [village-info form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit'" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/add-war-order"
              {:handler (partial add-war-order-complete village-info)
               :error-handler error-response
               :finally (partial form-finally2 form)
               :format :raw
               :params (merge {:village-loc (str (:loc village-info))}
                              form-values)})))

(defn force-types-received [village-info response]
  (ef/at "#content" (ef/content (add-war-order-view village-info)))
  (ef/at "#addwarorderform ul li:first-child" (ef/content ""))
  (ef/at "#addwarorderform ul" (ef/prepend
                                (map
                                 (fn [type]
                                   (force-value type))
                                 (:forces response))))
  (ef/at "#addwarorderform select[name='target']" (ef/content ""))
  (ef/at "#addwarorderform select[name='target']"
         (ef/content
          (map
           (fn [[loc name]]
             (select-option loc name))
           (filter
            (fn [[loc name]]
              (not= loc (:loc village-info)))
            (:available-targets response)))))
  (ef/at "#addwarorderform"
         (ev/listen :submit #(do (.preventDefault %)
                                 (send-add-war-order
                                  village-info
                                  (.-currentTarget %)))))
  (ef/at "#addwarorderform .cancel" (ev/listen :click #(go-to-village (:loc village-info)))))

(defn add-war-order-ui [village-info]
  (ajax/GET "/api/attack-types"
            {:handler (partial force-types-received village-info)}))

(defn add-training-order-complete [building response]
  (ef/at "#errorresponse" (ef/content ""))
  (ef/at "#addorderplace" (ef/content ""))
  (ef/at "#trainingorders ul"
         (ef/append (training-order-view (dissoc response :success)))))

(defn send-add-training-order [building form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit'" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/add-training-order"
              {:handler (partial add-training-order-complete building)
               :error-handler error-response
               :finally (partial form-finally2 form)
               :format :raw
               :params (merge {:building-id (:id building)}
                              form-values)})))

(defn warrior-types-received [building response]
  (ef/at "#errorresponse" (ef/content ""))
  (ef/at "#addorderplace" (ef/content (add-training-order-view)))
  (ef/at "#addtrainingorderform select[name='warrior']" (ef/content ""))
  (ef/at "#addtrainingorderform select[name='warrior']"
         (ef/content
          (map
           (fn [warrior]
             (select-option warrior warrior))
           (:warriors response))))
  (ef/at "#addtrainingorderform"
         (ev/listen :submit #(do (.preventDefault %)
                                 (send-add-training-order
                                  building
                                  (.-currentTarget %)))))
  (ef/at "#addtrainingorderform .cancel"
         (ev/listen :click #(do (ef/at "#errorresponse" (ef/content ""))
                                (ef/at "#addorderplace" (ef/content ""))))))

(defn add-training-order-ui [building]
  (ajax/GET "/api/warriors-for-building"
            {:handler (partial warrior-types-received building)
             :params {:subtype (str (:subtype building))}}))

(defn add-production-order-complete [building response]
  (ef/at "#errorresponse" (ef/content ""))
  (ef/at "#addorderplace" (ef/content ""))
  (ef/at "#productionorders ul"
         (ef/append (training-order-view (dissoc response :success)))))

(defn send-add-production-order [building form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit'" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/add-production-order"
              {:handler (partial add-production-order-complete building)
               :error-handler error-response
               :finally (partial form-finally2 form)
               :format :raw
               :params (merge {:building-id (:id building)}
                              form-values)})))

(defn product-types-received [building response]
  (ef/at "#errorresponse" (ef/content ""))
  (ef/at "#addorderplace" (ef/content (add-production-order-view)))
  (ef/at "#addproductionorderform select[name='product']" (ef/content ""))
  (ef/at "#addproductionorderform select[name='product']"
         (ef/content
          (map
           (fn [product]
             (select-option product product))
           (:products response))))
  (ef/at "#addproductionorderform"
         (ev/listen :submit #(do (.preventDefault %)
                                 (send-add-production-order
                                  building
                                  (.-currentTarget %)))))
  (ef/at "#addproductionorderform .cancel"
         (ev/listen :click #(do (ef/at "#errorresponse" (ef/content ""))
                                (ef/at "#addorderplace" (ef/content ""))))))

(defn add-production-order-ui [building]
  (ajax/GET "/api/products-for-building"
            {:handler (partial product-types-received building)
             :params {:subtype (str (:subtype building))}}))

(defn render-building-info [building]
  (ef/at "#content" (ef/content (building-internals-view building)))
  (ef/at "#addorderviews" (ef/content ""))
  (when (= :military (:type building))
    (ef/at "#trainingorders" (ef/remove-attr :style))
    (ef/at "#trainingorders ul" (ef/content
                                 (map
                                  (fn [training-order]
                                    (training-order-view training-order))
                                  (:training-orders building)))))
  (when (= :production (:type building))
    (ef/at "#productionorders" (ef/remove-attr :style))
    (ef/at "#productionorders ul" (ef/content
                                   (map
                                    (fn [production-order]
                                      (production-order-view production-order))
                                    (:production-orders building))))))

(defn go-to-building [building]
  (render-building-info building))

(defn render-village-info [village-info]
  (ef/at "#villageslist" (ef/content
                          (map
                           (fn [village]
                             (let [loc (str (nth village 0))
                                   name (str (nth village 1))]
                               (village-item loc name)))
                           (:villages village-info))))
  (ef/at "#content" (ef/content (village-view village-info)))
  (ef/at "#buildings ul" (ef/content
                          (map
                           (fn [building]
                             (building-view building))
                           (:buildings village-info))))
  (ef/at "#resources ul" (ef/content
                          (map
                           (fn [resource]
                             (resource-view resource))
                           (:cell-resources village-info))))
  (ef/at "#storage ul" (ef/content
                        (map
                         (fn [storage-item]
                           (storage-item-view storage-item))
                         (:storage village-info))))
  (ef/at "#forces ul" (ef/content
                       (map
                        (fn [[type puppet-ids]]
                          (force-view type puppet-ids))
                        (:forces village-info))))
  (ef/at "#warorders ul" (ef/content
                          (map
                           (fn [war-order]
                             (war-order-view war-order))
                           (:war-orders village-info))))
  (ef/at "#puppets ul" (ef/content
                        (map
                         (fn [puppet]
                           (puppet-view puppet))
                         (:puppets village-info)))))

(defn go-to-village [loc]
  (ajax/GET "/api/village-info"
            {:handler render-village-info
             :params {:loc (str loc)}}))

(defn show-village [village-info]
  (render-village-info village-info))
