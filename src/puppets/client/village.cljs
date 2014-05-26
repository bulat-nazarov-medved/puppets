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
  "#reloadvillage" (ev/listen :click #(go-to-village (:loc building))))

(em/deftemplate village-view "/prototype/village.html" []
  )

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

(defn render-building-info [building]
  (ef/at "#content" (ef/content (building-internals-view building)))
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
  (ef/at "#content" (ef/content (village-view)))
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
