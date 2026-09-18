(ns lambdaisland.compass.repl
  "REPL utility functions for quick maintenance tasks

  See also `bin/dev prod-repl`
  "
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass :as compass]
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
                         [(.contains ^String ?n ?n-e)]))]
         (db/db)
         name-or-email)))

(defn sessions []
  (map db/entity (db/q '[:find [?e ...]
                         :where
                         [?e :session/title]]
                       (db/db))))

(defn unassign-ticket [u]
  (let [user (if (string? u) (user u) u)]
    (if-let [ticket (u/assigned-ticket user)]
      (do
        @(db/transact [[:db/retract (:db/id ticket)
                        :tito.ticket/assigned-to (:db/id user)]])
        :ok)
      :not-assigned)))

(defn undo-accept [u]
  (let [user (if (string? u) (user u) u)]
    (if-let [uid (:db/id user)]
      (do
        @(db/transact [[:db/retract uid :privacy-policy/accepted-at]])
        :ok)
      :user-not-found)))

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

(defn find-ticket [s]
  (doseq [[id ref email] (db/q '[:find ?e ?ref ?email
                                 :where
                                 [?e :tito.ticket/reference ?ref]
                                 [?e :tito.ticket/email ?email]]
                               (db/db))]
    (when (or (str/includes? (str/lower-case ref) (str/lower-case s))
              (str/includes? (str/lower-case email) (str/lower-case s)))
      (print (str ref "\t" email "\tUser: "))
      (prn (some-> (db/entity id) :tito.ticket/assigned-to ((juxt :user/uuid :public-profile/name :discord/email))) ))))

(defn ig-config []
  (compass/ig-config))

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
                      :code "DUMZ"
                      :email "arne@arnebrasseur.net"
                      :name "Arne"})


  (map datomic.api/touch
       (:tito.ticket/_assigned-to
        (user "arne.brasseur@gmail.com")))

  (undo-accept "arne.brasseur@gmail.com")
  (unassign-ticket "arne@arnebrasseur.net")

  :privacy-policy/accepted-at
  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
