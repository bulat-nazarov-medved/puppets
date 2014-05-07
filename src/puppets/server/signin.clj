(ns puppets.server.signin
  (:use
   [puppets.server database mailer utils])
  (:require
   [metis.core :as mc]))

(defn freelogin [map key _]
  (when (user-exists? (key map))
    "user already exists"))

(defn race-correct [map key _]
  (when-not (contains? #{"clojure" "groovy" "scala"} (key map))
    "race must be one of 'clojure', 'groovy' or 'scala'"))

(mc/defvalidator signin-data-validator
  [:username [:presence :freelogin]]
  [:email [:presence :email]]
  [:password [:presence
              :confirmation {:confirm :password-confirmation}]]
  [:race [:presence :race-correct]])

(defn create-user
  [username email password race]
  (let [activation-code (hexadecimalize (generate-secure-token 32))
        uid (user-create! username email password race activation-code)
        mail-sent-status (try
                           (mail email "Puppets [Account activation]"
                                 (str "Hi, " username "!!!\n\n"
                                      "To activate your 'Puppets' account "
                                      "just follow the link below: \n\n"
                                      (System/getenv "BASE_URL") "/activate/"
                                      activation-code))
                           (catch Exception e
                             {:error :EXCEPTION :exception e}))]
    (if (= :SUCCESS (:error mail-sent-status))
      (do (user-mark-email-sent! uid)
          {:status :complete})
      {:status :email-not-sent :mail-sent-status mail-sent-status})))

(defn signin
  [{:keys [username email password password-confirmation race] :as params}]
  (let [tests (signin-data-validator params)]
    (if (empty? tests)
      (create-user username email password race)
      {:status :tests-error :tests tests})))
