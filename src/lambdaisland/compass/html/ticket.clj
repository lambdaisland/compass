(ns lambdaisland.compass.html.ticket
  (:require
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]))


(o/defstyled connect-ticket-form :form
  ([ref email]
   [:<> {:method "post" :action (url-for :ticket/connect)}
    [:label {:for "reference"} [:span "Reference code on your Ti.to ticket"]
     [:input#reference (cond-> {:type "text" :required true :name "reference" :maxlength 6 :placeholder "ABCD-1"}
                         ref (assoc :value ref))]]
    [:label {:for "email"} [:span "The email address this ticket is assigned to"]
     [:input#email {:type "email" :required true :name "email" :value email}]]
    [:input {:type "submit" :value "Claim Ticket"
             :cx-enabled-by "#reference, #email"}]]))

(o/defstyled connect-ticket-page :section.form-card-styling
  ([error-message ref email]
   [:<>
    (when error-message
      [:p {:style "color: red;"} error-message])
    [:h2 "Ticket Check"]
    [:p "Claim your Ti.to conference ticket!"]
    [:p "This will unlock full access to both Confpass.me and Discord."]
    [:p "Make sure the ticket is assigned in Ti.to, and that the email address matches the one on the ticket."]
    [connect-ticket-form ref email]])
  ([ref email]
   (connect-ticket-page nil ref email)))

(o/defstyled confirm-ticket-form :section.form-card-styling
  [:code :p-0]
  ["input[type='checkbox']" {:height "1.33em"}]
  [:.checkbox [:> [:span:last-child {:align-items :flex-start}]]]
  [:.assigned {:text-decoration "line-through"}]
  ([email tickets]
   [:<>
    [:h2 "Ticket Confirmation"]
    [:p "Attach these tickets to your Confpass.me account:"]
    [:form.ticket-confirmation {:method "post" :action (url-for :ticket/confirm)}
     [:input {:type "hidden" :name "email" :value email}]
     (for [{:tito.ticket/keys [reference email release assigned-to]} tickets]
       [:label.checkbox {:for reference}
        [:span
         [:input {:id reference
                  :name "reference"
                  :value reference
                  :type "checkbox"
                  :disabled (boolean assigned-to)
                  :checked (not assigned-to)}]
         [:div 
          [:p {:class (when assigned-to "assigned")} [:code reference] " " (:tito.release/title release) " " [:code "(" email ")"]] 
          (when assigned-to
            [:span "This ticket has already been assigned to " [c/inline-user assigned-to]])]]])
     [:p "If you have additional tickets in a separate order you can add them in the next step."]
     [:input {:type "submit" :value "Confirm"
              :cx-enabled-by ".ticket-confirmation input[type=checkbox]"
              :cx-enabled-by-policy "one"}]]]))

(o/defstyled your-tickets-page :div
  :flex :flex-col :gap-3
  [:.tickets
   :flex-col
   :gap-2]
  [:.ticket
   :flex-row
   {:background-color t/--surface-2
    :padding t/--size-3
    :border-radius t/--size-3
    :gap t/--size-2}]
  [:.info :flex-grow]
  ([tickets]
   [:<>
    [:h2 "Your Tickets"]
    [:section.tickets
     (if (seq tickets)
       (for [{:tito.ticket/keys [reference name email release] :as ticket} tickets]
         [:div.ticket
          [:div.ref [:code reference]]
          [:div.info 
           [:div.release (:tito.release/title release)]
           [:div.email name [:code "<" email ">"]]]
          [:div.actions
           [:button {:hx-confirm (str "Removing ticket " reference " " (:tito.release/title release) ", are you sure?")
                     :hx-delete (url-for :ticket/ticket {:ticket-id (:db/id ticket)})}
            "Remove"]]])
       [:p "You don't have any assigned tickets."])]
    [:section.form.form-card-styling
     [:h3 "Add an additional ticket"]
     [:p "Make sure the ticket is assigned in Ti.to, and that the email address matches the one on the ticket."]
     [connect-ticket-form nil nil]]]))
