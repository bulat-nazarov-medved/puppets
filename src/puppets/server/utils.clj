(ns puppets.server.utils
  (:require
   [clojure.string :as str])
  (:import
   [java.security SecureRandom]))

(defn hexadecimalize [a-byte-array]
  (str/lower-case (apply str (map #(format "%02X" %) a-byte-array))))

(defn generate-secure-token [size]
  (let [seed (byte-array size)]
    (.nextBytes (SecureRandom/getInstance "SHA1PRNG") seed)
    seed))
