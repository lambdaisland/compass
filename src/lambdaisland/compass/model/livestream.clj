(ns lambdaisland.compass.model.livestream
  (:require
   [lambdaisland.compass.model.user :as user]))

(defn ticket-release-slug [identity]
  (some-> identity
          user/assigned-ticket
          :tito.ticket/release
          :tito.release/slug))

(defn accessible? [identity stream]
  (contains? (:allowed-ticket-slugs stream)
             (ticket-release-slug identity)))

(defn accessible-streams [identity streams]
  (filterv #(accessible? identity %) streams))
