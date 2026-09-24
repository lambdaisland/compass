(ns lambdaisland.compass.routes.ticket
  (:require
   [lambdaisland.compass.db :as db]
   [lambdaisland.compass.html.ticket :as ticket]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.compass.services.discord :as discord]
   [lambdaisland.compass.services.tito :as tito]))

(defn GET-connect-ticket-form
  [{:keys [identity] :as req}]
  {:html/body [ticket/connect-ticket-page nil (:discord/email identity)]})

(defn POST-connect-ticket-form
  [{:keys                           [identity]
    {ref "reference" email "email"} :form-params
    :as                             req}]
  (if (and ref email)
    (let [error-response  (fn [status & msg] {:html/body [ticket/connect-ticket-page msg ref email]})
          tickets         (tito/find-tickets ref email)]
      
      (cond
        ;; Did ref + email yield one or more ticket? If not, give some feedback.
        ;; It's a common mistake to not assign the ticket in Ti.to.
        (empty? tickets)
        (if (seq (tito/find-tickets-by-ref ref))
          (error-response 404 "There is a Ti.to order with registration
          code " [:code ref] ", but it does not match the provided email
          address. Make sure your ticket has been assigned in Ti.to, and that
          the email address provided here matches the one on the ticket.")
          (error-response 404 "Registration reference " [:code ref] " not found."))

        ;; At this point we know ref + email match, but we need to be careful to
        ;; assign the right ticket, ask for confirmation
        :else
        {:html/body
         [ticket/confirm-ticket-form email tickets]})

      #_
      
      (cond
        (:tito.ticket/_assigned-to identity)
        (error-response 409 "A ticket is already assigned to your account!")

        assigned-ticket
        (if (discord/assign-ticket-roles (:discord/id identity) assigned-ticket)
          (do
            @(db/transact
              [[:db/add (:db/id assigned-ticket) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]]])
            (response/redirect
             "/"
             {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))
          (error-response 500 "Your data is correct, but the ticket roles could not be assigned to you. This is a bug; please contact the administrators."))

        (empty? tickets)
        (error-response 404 "Registration reference " [:code ref] " not found.")

        (seq unassigned-refs)
        (error-response 404
                        "Registration " [:code ref] " found, but no ticket in it is assigned to your email address."
                        [:br] "The following tickets are not assigned to an email address yet (maybe you forgot to assign one of them to " [:code email] "?): "
                        (str/join "," unassigned-refs))

        :else
        (error-response 404 "Registration " [:code ref] " found, but no ticket in it is assigned to your email address.")))

    {:status    400
     :html/body "Missing parameters"}))

(defn POST-confirm-ticket-form [{:keys [form-params identity] :as req}]
  (let [{:strs [email reference]} form-params
        references (if (vector? reference) reference [reference])
        tickets (tito/find-free-tickets-by-refs references email)]
    (doseq [t tickets]
      (discord/assign-ticket-roles (:discord/id identity) t))
    @(db/transact
      (for [t tickets]
        [:db/add (:db/id t) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]]))
    (response/redirect (url-for :ticket/overview))))

(defn GET-ticket-overview [{:keys [identity] :as req}]
  {:html/body
   [ticket/your-tickets-page (:tito.ticket/_assigned-to identity)]})

(defn DELETE-ticket [{:keys [path-params identity] :as req}]
  (let [{:keys [ticket-id]} path-params]
    @(db/transact [[:db/retract (parse-long ticket-id) :tito.ticket/assigned-to (:db/id identity)]])
    {:location (url-for :ticket/overview)}))

(defn routes []
  ["/ticket"
   {:middleware [[response/wrap-requires-auth]]}
   ["/overview"
    {:name :ticket/overview
     :get {:handler #'GET-ticket-overview}}]
   ["/t/:ticket-id"
    {:name :ticket/ticket
     :delete {:handler #'DELETE-ticket}}]
   ["/connect"
    {:name :ticket/connect
     :get {:handler #'GET-connect-ticket-form}
     :post {:handler #'POST-connect-ticket-form}}]
   ["/confirm"
    {:name :ticket/confirm
     :post {:handler #'POST-confirm-ticket-form}}]])
