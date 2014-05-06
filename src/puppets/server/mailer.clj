(ns puppets.server.mailer
  (:use
   [postal core])
  (:require
   [puppets.server.aes :as aes]))

(defn mail [to subject body]
  (send-message {:host (System/getenv "SMTP_HOST")
                 :user (System/getenv "SMTP_USER")
                 :pass (aes/decrypt (System/getenv "SMTP_PASS") "puppets-key")
                 :tls :yes}
                {:from "robot"
                 :to [to]
                 :subject subject
                 :body body}))
