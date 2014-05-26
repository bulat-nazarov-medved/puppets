(ns puppets.client.core
  (:require
   [ajax.core :as ajax]
   [enfocus.core :as ef]
   [enfocus.events :as ev]
   [puppets.client.village :as v])
  (:require-macros
   [enfocus.macros :as em]))

(em/deftemplate signin-html "/prototype/signin.html" [])

(em/deftemplate login-html "/prototype/login.html" [])

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

(defn village-info-complete [response]
  (v/show-village response))

(defn logged-in []
  (ef/at "#logout" (ef/remove-attr :style))
  (ef/at "#login" (ef/set-attr :style "display:none"))
  (ef/at "#signin" (ef/set-attr :style "display:none"))
  (ajax/GET "/api/village-info"
            {:handler village-info-complete}))

(defn login-complete [response]
  (.log js/console "lc")
  (.log js/console (str response))
  (if (= :success (:status response))
    (do (logged-in)
        (close-dialog))
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

(defn logged-out []
  (ef/at "#logout" (ef/set-attr :style "display:none"))
  (ef/at "#login" (ef/remove-attr :style))
  (ef/at "#signin" (ef/remove-attr :style))
  (ef/at "#content" (ef/content ""))
  (ef/at "#villageslist" (ef/content ""))
  (ef/at "#eventslist" (ef/content "")))

(defn logout-complete [response]
  (logged-out))

(defn logout []
  (ajax/GET "/api/logout"
            {:handler logout-complete
             :format :raw}))

(em/deftemplate main-html "/prototype/main.html" []
  "#content > *" (ef/remove-node)
  "#villageslist > *" (ef/remove-node)
  "#eventslist > *" (ef/remove-node)
  "#login" (ev/listen :click show-login)
  "#logout" (ev/listen :click logout)
  "#signin" (ev/listen :click show-signin)
  "#worldstep" (ev/listen :click #(ajax/GET "/api/step")) ;; for testing purposes
  )

(defn init []
  (ef/at "body" (ef/content (main-html)))
  (if js/userlogged
    (logged-in)
    (logged-out)))

(set! (.-onload js/window) #(em/wait-for-load (init)))
