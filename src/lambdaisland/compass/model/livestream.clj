(ns lambdaisland.compass.model.livestream
  "Livestream related logic, and ticket-based access control"
  (:require
   [lambdaisland.compass.model.user :as user]))

(defn ticket-release-slugs [identity]
  (set 
   (map (comp :tito.release/slug
              :tito.ticket/release)
        (user/assigned-tickets identity))))

(defn accessible? [identity stream]
  (some (ticket-release-slugs identity)
        (:livestream/allowed-ticket-slugs stream)))

(defn accessible-streams [identity streams]
  (filterv #(accessible? identity %) streams))

(defn ticket-streams [ticket streams]
  (filterv #(contains? (:livestream/allowed-ticket-slugs streams)
                       (-> ticket :tito.ticket/release :tito.release/slug)) streams))
