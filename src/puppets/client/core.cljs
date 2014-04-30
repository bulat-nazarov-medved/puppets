(ns puppets.client.core
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(defn init []
  (ef/at "body" (ef/content "Hello world!!")))

(set! (.-onload js/window) #(em/wait-for-load (init)))
