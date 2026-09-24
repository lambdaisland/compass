(ns lambdaisland.compass.model.user
  "Functions relating to user entities"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.model.assets :as assets]
   [hato.client :as hato]))

(defn avatar-css-value [user]
  (if-let [url (:public-profile/avatar-url user)]
    (str "url(" (assets/image-url url) ")")
    (str "var(--gradient-" (inc (mod (:db/id user) 7)) ")")))

(defn assigned-tickets
  [user]
  (:tito.ticket/_assigned-to user))

(defn admin? [user]
  (when-let [tickets (seq (assigned-tickets user))]
    (some (into #{} (keep #(-> % :tito.ticket/release :tito.release/slug)) tickets)
          (config/value :tito/admin-slugs))))
