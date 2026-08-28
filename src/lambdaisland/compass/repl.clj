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

(defn make-dummy-ticket [{:keys [release code email name]}]
  (let [release-id (db/q '[:find ?r .
                           :in $ ?s
                           :where [?r :tito.release/slug ?s]]
                         (db/db) release)]
    @(db/transact
      [{:tito.ticket/reference (str code "-1")
        :tito.ticket/name      name
        :tito.ticket/email     email
        :tito.ticket/release   release-id
        :tito.ticket/state     "complete"
        :tito.ticket/registration
        {:tito.registration/reference code
         :tito.registration/email     email
         :tito.registration/name      name
         :tito.registration/state     "complete"}}])))


(comment
  (db/q '[:find (pull ?e [*])
          :where [?e :tito.ticket/id]]
        (db/db))

  (db/q '[:find (pull ?e [*])
          :where [?e :tito.registration/id]]
        (db/db))
  (db/q '[:find (pull ?e [*])
          :where [?e :tito.release/id]]
        (db/db))

  (make-dummy-ticket {:release "comp-ticket"
                      :code "DUMM"
                      :email "arne@arnebrasseur.net"
                      :name "Arne"})

  (map datomic.api/touch
       (:tito.ticket/_assigned-to
        (user "arne.brasseur@gmail.com")))

  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
