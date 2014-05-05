(ns puppets.client.core
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(em/deftemplate main-html "/prototype/main.html" [])

(defn init []
  (ef/at "body" (ef/content (main-html))))

(set! (.-onload js/window) #(em/wait-for-load (init)))
