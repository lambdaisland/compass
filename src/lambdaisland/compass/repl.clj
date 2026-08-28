(ns lambdaisland.compass.repl
  "REPL utility functions for quick maintenance tasks

  See also `bin/dev prod-repl`
  "
  (:require
   [lambdaisland.compass.db :as db :refer :all]
   [lambdaisland.compass.model.user :as u]))

(comment
  (require 'lambdaisland.compass.repl)
  (in-ns 'lambdaisland.compass.repl)
  )

(defn user [name-or-email]
  (db/entity
   (db/q '[:find ?e .
           :in $ ?n-e
           :where
           (or-join [?e ?n-e]
                    (and [?e :public-profile/name ?n]
                         [(.contains ^String ?n ?n-e)])
                    (and [?e :discord/email ?n]
                         [(.contains ^String ?n ?n-e)]))
           ]
         (db/db)
         name-or-email)))

(defn sessions []
  (map db/entity (db/q '[:find [?e ...]
                         :where
                         [?e :session/title]]
                       (db/db))))

(defn unassign-ticket [user]
  @(db/transact [[:db/retract (:db/id (u/assigned-ticket user)) :tito.ticket/assigned-to (:db/id user)]]))

(comment

  (user "arne.brasseur@gmail.com")

  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
