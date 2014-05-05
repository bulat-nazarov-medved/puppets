(ns puppets.client.core
  (:require
   [ajax.core :as ajax]
   [enfocus.core :as ef]
   [enfocus.events :as ev])
  (:require-macros
   [enfocus.macros :as em]))

(em/deftemplate signin-html "/prototype/signin.html" [])

(em/deftemplate main-html "/prototype/main.html" []
  "#content > *" (ef/remove-node)
  "#villageslist > *" (ef/remove-node)
  "#eventslist > *" (ef/remove-node)
  "#signin" (ev/listen :click show-signin)
  )

(defn show-dialog []
  (ef/at "#content" (ef/set-attr :style "display:none"))
  (ef/at "#dialog" (ef/remove-attr :style)))

(defn close-dialog []
  (ef/at "#dialog" (ef/content ""))
  (ef/at "#dialog" (ef/set-attr :style "display:none"))
  (ef/at "#content" (ef/remove-attr :style)))

(defn send-signin [form]
  (let [form-values (ef/from form (ef/read-form))]
    
    (.log js/console (pr-str form-values))))

(defn show-signin [event]
  (ef/at "#dialog" (ef/content (signin-html)))
  (ef/at "#signinform" (ev/listen :submit #(do (.preventDefault %)
                                               (send-signin
                                                (.-currentTarget %)))))
  (ef/at ".cancel" (ev/listen :click #(close-dialog)))
  (show-dialog))

(defn init []
  (.log js/console (ajax/GET "/api/login"))
  (ef/at "body" (ef/content (main-html)))
  (if js/userlogged
    (do
      (ef/at "#login" (ef/remove-node))
      (ef/at "#signin" (ef/remove-node)))
    (ef/at "#logout" (ef/remove-node)))
  (ef/at "#content" (ef/content (str js/userlogged))))

;; (comment
;;   (ajax/GET "/api/login"
;;             {:handler #(ef/at "#content"
;;                               (ef/content (str %)))}))

(set! (.-onload js/window) #(em/wait-for-load (init)))
