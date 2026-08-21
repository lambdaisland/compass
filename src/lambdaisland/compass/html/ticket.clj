(ns lambdaisland.compass.html.ticket
  (:require
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defstyled connect-ticket-form :section
  [:label :block]
  [#{:h2 :p :label} {:margin-bottom t/--size-3}]
  [:label :flex :flex-col
   [:span :font-semibold]]
  ([error-message ref email]
   [:<>
    (when error-message
      [:p {:style "color: red;"} error-message])
    [:h2 "Ti.to Checkin"]
    [:p "Claim your conference ticket!"]
    [:p "This will unlock full access to both Confpass and Discord"]
    [:form {:method "post"}
     [:label {:for "reference"} [:span "Reference code on your Ti.to ticket"]
      [:input#reference (cond-> {:type "text" :required true :name "reference" :maxlength 6 :placeholder "ABCD-1"}
                          ref (assoc :value ref))]]
     [:label {:for "email"} [:span "The email address this ticket is assigned to"]
      [:input#email {:type "email" :required true :name "email" :value email}]]
     [:input {:type "submit" :value "Claim Ticket"}]]])
  ([ref email]
   (connect-ticket-form nil ref email)))

(o/defstyled ticket-connected :div
  :flex :flex-col :gap-3
  ([ticket]
   [:<>
    [:h2 "Ticket Connection"]
    [:p "Your ti.to ticket has been connected! You're all set!"]
    [:p "Your ticket reference is " [:strong (:tito.ticket/reference ticket)] "."]]))
