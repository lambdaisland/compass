(ns lambdaisland.compass.routes.contacts
  (:require
   [clj.qrgen :as qr]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.db :as db]
   [lambdaisland.compass.db.queries :as q]
   [lambdaisland.compass.html.contacts :as h]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.compass.model.assets :as assets]
   [lambdaisland.compass.model.attendees :as attendees]
   [ring.util.response :as ring-response]))

(defn GET-contact-list
  "Show the private contact list of the user.
    - Users can revoke their contacts in this page"
  [req]
  {:html/body [h/contact-detail
               (:identity req)]})

(defn eid->qr-uuid
  "create an uuid as the hash for eid to prevent guessing
   store this uuid in the user"
  [user-eid]
  (let [qr-uuid (random-uuid)]
    @(db/transact [{:db/id user-eid
                    :user/hash qr-uuid}])
    qr-uuid))

(defn qr-uuid->eid
  "Accept a uuid type's qr-uuid, and use it to query the
   user's eid"
  [qr-uuid]
  (db/q '[:find ?e .
          :in $ ?hash
          :where
          [?e :user/hash ?hash]]
        (db/db)
        (if (string? qr-uuid)
          (parse-uuid qr-uuid)
          qr-uuid)))

(defn GET-qr-html [req]
  {:html/body [h/qr-dialog]
   :html/layout false})

(defn GET-qr-code
  [{:keys [identity] :as req}]
  (let [user-eid (:db/id identity)
        host (config/value :compass/origin)
        qr-uuid (str (eid->qr-uuid user-eid))
        url (str host (url-for :contact/add {:qr-uuid qr-uuid}))
        qr-image (qr/as-bytes (qr/from url :size [400 400]))]
    (-> (ring-response/response qr-image)
        (assoc-in [:headers "content-type"] "image/png"))))

(defn GET-attendees [req]
  (let [attendees (q/all-users)]
    {:html/body
     [:<>
      [:p "The Attendees List"]
      (for [atd (attendees/user-list attendees)]
        (h/attendee-card atd))]}))

(defn GET-contact
  [req]
  (let [qr-uuid     (get-in req [:path-params :qr-uuid])
        contact-eid (qr-uuid->eid qr-uuid)
        contact     (db/entity contact-eid)]
    {:html/body [h/accept-invite-html qr-uuid contact]}))

(defn DELETE-contact
  [req]
  (let [me-id (:db/id (:identity req))
        contact-id (parse-long (get-in req [:path-params :id]))]
    @(db/transact [[:db/retract me-id :user/contacts contact-id]
                   [:db/retract contact-id :user/contacts me-id]])
    {:location :contacts/index
     :hx/trigger "contact-deleted"}))

(defn POST-contact
  "Part of the url is hash of the contact's user eid
   Decode it and add that contact"
  [{:keys [identity] :as req}]
  (let [user-eid (:db/id identity)
        qr-uuid (parse-uuid (get-in req [:path-params :qr-uuid]))
        contact-eid (qr-uuid->eid qr-uuid)
        ;; According to the schema
        ;; A :u/c B means that user A agrees to show their public profile to user B.
        ;; contact -> A
        ;; user -> B
        _ @(db/transact [{:db/id contact-eid
                          :user/contacts user-eid}
                         {:db/id user-eid
                          :user/contacts contact-eid}])]
    {:location :contacts/index}))

(defn routes []
  [["/contact"
    {:middleware [[response/wrap-requires-auth]]}
    ["/qr" {:name :contact/qr
            :get {:handler GET-qr-html}}]
    ["/qr.png" {:name :contact/qr-png
                :get {:handler GET-qr-code}}]
    ["/:qr-uuid"
     {:name :contact/add
      :post       {:handler POST-contact}
      :get        {:handler GET-contact}}]
    ["/link/:id"
     {:name :contact/link
      :delete     {:handler DELETE-contact}}]]
   ["/contacts"
    {:middleware [[response/wrap-requires-auth]]}
    ["/" {:name :contacts/index
          :get {:handler GET-contact-list}}]]
   #_["/attendees"
      [""
       {:name :attendees/index
        :middleware [[response/wrap-requires-auth]]
        :get        {:handler GET-attendees}}]]])

(comment

  (qr-uuid->eid #uuid "5243e338-b6d5-4517-a24f-1205bf9f4604")

  (clojure.java.browse/browse-url
   (str "http://localhost:8099/contact/"
        (eid->qr-uuid
         (db/q '[:find ?e .
                 :in $ ?n-e
                 :where
                 (or-join [?e ?n-e]
                          (and [?e :public-profile/name ?n]
                               [(.contains ^String ?n ?n-e)])
                          (and [?e :discord/email ?n]
                               [(.contains ^String ?n ?n-e)]))]
               (db/db)
               "arne@lambdaisland.com"))))
  )
