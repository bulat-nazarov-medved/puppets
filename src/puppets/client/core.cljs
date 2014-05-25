(ns puppets.client.core
  (:require
   [ajax.core :as ajax]
   [enfocus.core :as ef]
   [enfocus.events :as ev])
  (:require-macros
   [enfocus.macros :as em]))

(em/deftemplate signin-html "/prototype/signin.html" [])

(em/deftemplate login-html "/prototype/login.html" [])

(em/deftemplate main-html "/prototype/main.html" []
  "#content > *" (ef/remove-node)
  "#villageslist > *" (ef/remove-node)
  "#eventslist > *" (ef/remove-node)
  "#login" (ev/listen :click show-login)
  "#signin" (ev/listen :click show-signin)
  )

(defn show-dialog []
  (ef/at "#content" (ef/set-attr :style "display:none"))
  (ef/at "#dialog" (ef/remove-attr :style)))

(defn close-dialog []
  (ef/at "#dialog" (ef/content ""))
  (ef/at "#dialog" (ef/set-attr :style "display:none"))
  (ef/at "#content" (ef/remove-attr :style)))

(defn form-finally [form]
  (ef/at form "input[type='submit']" (ef/remove-attr :disabled)))

(defn signin-complete [response]
  (if (= :tests-error (:status response))
    (ef/at "#signinresponse" (ef/content (str (:tests response))))
    (do (close-dialog)
        (ef/at "#content" (ef/content "Your activation code sent to your email.")))))

(defn signin-error [response]
  (ef/at "#signinresponse" (ef/content (str "error: " response))))

(defn send-signin [form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit']" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/signin"
              {:handler signin-complete
               :error-handler signin-error
               :finally (partial form-finally form)
               :format :raw
               :params form-values})))

(defn show-signin [event]
  (ef/at "#dialog" (ef/content (signin-html)))
  (ef/at "#signinform" (ev/listen :submit #(do (.preventDefault %)
                                               (send-signin
                                                (.-currentTarget %)))))
  (ef/at ".cancel" (ev/listen :click #(close-dialog)))
  (show-dialog))

(defn login-complete [response]
  (.log js/console "lc")
  (.log js/console (str response))
  (if (= :success (:status response))
    (do)
    (ef/at "#loginresponse" (ef/content (:description response)))))

(defn login-error [response]
  (ef/at "#loginresponse" (ef/content (str "error: " response))))

(defn send-login [form]
  (let [form-values (ef/from form (ef/read-form))]
    (ef/at form "input[type='submit'" (ef/set-attr :disabled :disabled))
    (ajax/GET "/api/login"
              {:handler login-complete
               :error-handler login-error
               :finally (partial form-finally form)
               :format :raw
               :params form-values})))

(defn show-login [event]
  (ef/at "#dialog" (ef/content (login-html)))
  (ef/at "#loginform" (ev/listen :submit #(do (.preventDefault %)
                                              (send-login
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

(set! (.-onload js/window) #(em/wait-for-load (init)))
