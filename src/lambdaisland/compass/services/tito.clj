(ns lambdaisland.compass.services.tito
  "Ti.to API calls and import logic

  We use this to fetch information about registered tickets, so we know who is a
  ticket holder, and what type of ticket.

  In dev by default reads `data/tito/*.edn` instead of doing real API calls, set
  `:tito/fake-api-stubs?` to false and config `:tito/api-key` to make it use the
  actual API.
  "
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.db :as db]
   [lambdaisland.compass.util :as util]
   [hato.client :as hato]
   [integrant.core :as ig]
   [io.pedestal.log :as log]))

(def API_ENDPOINT (str "https://api.tito.io/v3/" (config/value :tito/event-slug) "/"))

(defn tito-url [path]
  (if (vector? path)
    (recur (str/join "/" path))
    (str API_ENDPOINT path)))

(defn tito-request [opts]
  (util/deep-dasherize-keys
   (:body
    (hato/request
     (-> opts
         (assoc-in [:headers "Authorization"]
                   (str "Token token=" (config/value :tito/api-key)))
         (assoc
           :url (tito-url (:path opts))
           :as :auto
           :content-type :json
           :accept :json))))))

(defn tito-get [path opts] (tito-request (assoc opts :path path :request-method :get)))
(defn tito-post [path opts] (tito-request (assoc opts :path path :request-method :post)))
(defn tito-put [path opts] (tito-request (assoc opts :path path :request-method :put)))
(defn tito-patch [path opts] (tito-request (assoc opts :path path :request-method :patch)))

(defn tito-get-seq [endpoint key & [params]]
  (if (config/value :tito/fake-api-stubs?)
    (read-string (slurp (io/file "data" "tito" (str endpoint ".edn"))))
    (let [result (tito-get endpoint {:query-params params})
          data (get result key)
          next-page (get-in result [:meta :next-page])
          tail (if next-page
                 (lazy-seq
                  (tito-get-seq
                   endpoint
                   key
                   (assoc params :page next-page)))
                 nil)]
      (reduce #(cons %2 %1) tail (reverse data)))))

(defn fetch-registrations
  "Get 'registrations', what in the UI are called Orders. A registration can have
  multiple tickets."
  []
  (tito-get-seq "registrations" :registrations))

(defn fetch-tickets
  "Get individual tickets across all orders."
  []
  (tito-get-seq "tickets" :tickets))

(defn fetch-releases []
  (tito-get-seq "releases" :releases))

(defn registrations-tx []
  (for [{:keys [id reference email name state]} (fetch-registrations)]
    {:tito.registration/id id
     :tito.registration/reference reference
     :tito.registration/email email
     :tito.registration/name name
     :tito.registration/state state}))

(defn tickets-tx []
  (for [{:keys [id reference name email registration-id release-id state]} (fetch-tickets)]
    (cond->
        {:tito.ticket/id           id
         :tito.ticket/reference    reference
         :tito.ticket/registration [:tito.registration/id registration-id]
         :tito.ticket/release      [:tito.release/id release-id]
         ;; if state == "new" or "reminder", then the ticket hasn't been assigned,
         ;; and if someone tries to use it we should tell them to assign the ticket
         ;; in ti.to first. If it's "complete" or "incomplete" it's fine, incomplete
         ;; just means there are questions they haven't answered yet.
         :tito.ticket/state        state}
      ;; Note that even for "complete" tickets these can be blank, because some
      ;; special ticket types like "Opportunity Scholarship Donation" don't need
      ;; to be assigned.
      (not (str/blank? email))
      (assoc :tito.ticket/email email)
      (not (str/blank? name))
      (assoc :tito.ticket/name name))))

(defn releases-tx []
  (for [{:keys [id title slug]} (fetch-releases)]
    {:tito.release/id id
     :tito.release/title title
     :tito.release/slug slug}))

(defn sync! []
  @(db/transact
    (concat (releases-tx)
            (registrations-tx)))
  @(db/transact (tickets-tx)))

(defn find-tickets
  "Look up tickets by registration reference (4 character code) + email

  This drops the numeric suffix off of the reference, if present, and normalizes
  email addresses (trim+lower-case) both on the input and in the ti.to data.
  
  Will return all tickets in the order assigned to the given email address."
  [ref email]
  (let [ref (subs (str/upper-case ref) 0 4)
        email (str/trim (str/lower-case email))]
    (db/entities
     '[:find
       [?ticket ...]
       :in $ ?ref ?email
       :where
       [?reg :tito.registration/reference ?ref]
       [?ticket :tito.ticket/registration ?reg]
       [?ticket :tito.ticket/email ?tito-email]
       [(clojure.string/lower-case ?tito-email) ?tito-email1]
       [(clojure.string/trim ?tito-email1) ?tito-email2]
       [(= ?email ?tito-email2)]]
     (db/db)
     ref
     email)))

(defn find-tickets-by-ref
  "Look up tickets by registration reference (4 character code+suffix).

  Will validate that the email matches the email on the ticket, and that they
  are not yet assigned. 
  
  Returns ticket entities."
  [ref email]
  (let [ref (subs (str/upper-case ref) 0 4)]
    (db/entities
     '[:find
       [?ticket ...]
       :in $ ?ref ?email
       :where
       [?reg :tito.registration/reference ?ref]
       [?ticket :tito.ticket/registration ?reg]
       [?ticket :tito.ticket/email ?tito-email]
       [(clojure.string/lower-case ?tito-email) ?tito-email1]
       [(clojure.string/trim ?tito-email1) ?tito-email2]
       [(= ?email ?tito-email2)]]
     (db/db)
     ref
     email)))

(defn find-free-tickets-by-refs
  "Look up tickets by registration reference (4 character code+suffix).

  Will validate that the email matches the email on the ticket, and that they
  are not yet assigned. 
  
  Returns ticket entities."
  [references email]
  (db/entities
   '[:find
     [?ticket ...]
     :in $ [?ref ...] ?email
     :where
     [?ticket :tito.ticket/reference ?ref]
     [?ticket :tito.ticket/email ?tito-email]
     [(clojure.string/lower-case ?tito-email) ?tito-email1]
     [(clojure.string/trim ?tito-email1) ?tito-email2]
     [(= ?email ?tito-email2)]
     (not [?ticket :tito.ticket/assigned-to])]
   (db/db)
   references
   email))

(defn find-tickets-by-email
  "Look up tickets by email address on the ticket.

  Returns a vector of found ticket maps (including release information)."
  [email]
  (db/q
   '[:find
     [(pull ?ticket [* {:tito.ticket/release [*]}]) ...]
     :in $ ?email
     :where
     [?ticket :tito.ticket/email ?e0]
     [(clojure.string/lower-case ?e0) ?e1]
     [(clojure.string/trim ?e1) ?e2]
     [(= ?email ?e2)]]
   (db/db)
   (str/trim (str/lower-case email))))

(defn assigned-in-tito?
  "Has this ticket been assigned to someone in ti.to, i.e. does it have an email address

  The alternative is that it is \"new\" or \"reminder\"."
  [ticket]
  (-> ticket :tito.ticket/state #{"complete" "incomplete"}))

(defmethod ig/init-key :tito/sync [_ {:keys [interval-seconds]}]
  (log/info :tito/starting-sync-loop {:interval-seconds interval-seconds})
  (let [stop? (volatile! false)]
    (future
      (while (not @stop?)
        (try
          (sync!)
          (catch Exception e
            (log/error :tito/sync-failed {} :exception e)))
        (Thread/sleep (* 1000 interval-seconds))))
    stop?))

(defmethod ig/halt-key! :tito/sync [_ stop?]
  (vreset! stop? true))

(comment
  (sync!)

  ;; deps.local.edn
  ;; {:deps {com.lambdaisland/faker {:mvn/version "RELEASE"}}}

  (spit "data/tito/releases.edn"
        (util/pprint-str
         (mapv #(select-keys % [:id :title :slug]) (fetch-releases))))

  (require '[lambdaisland.faker :as faker])

  (let [releases (fetch-releases)
        registrations
        (for [_ (range 10)]
          (faker/fake {:id (partial rand-int 9999999)
                       :reference #"[A-Z0-9]{4}"
                       :email [:internet :email]
                       :name [:name :name]
                       :state "complete"}))
        tickets
        (for [{:keys [reference id]} registrations
              i (map inc (range (rand-nth [1 1 1 2 3])))]
          (faker/fake {:id (partial rand-int 9999999)
                       :reference (str reference "-" i)
                       :email [:internet :email]
                       :name [:name :name]
                       :state #{"reminder" "complete" "new" "incomplete"}
                       :registration-id id
                       :release-id (into #{} (map :id releases))}))
        ]
    (spit "data/tito/registrations.edn" (util/pprint-str registrations))
    (spit "data/tito/tickets.edn" (util/pprint-str tickets))
    ))
