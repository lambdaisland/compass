(ns lambdaisland.compass.html.welcome
  (:require
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.compass.config :as config]))


(o/defstyled welcome-page :div
  :flex :flex-col :gap-4
  [#{:p :label} {:font-size t/--font-size-4}]
  [:form :flex :flex-col :gap-4]
  ["[type=checkbox]" {:margin-right t/--size-2}]
  ["[type=submit]" {:background-color t/--highlight}
   ["&[disabled]" {:background-color t/--gray-5}]]

  ([identity]
   [:<>
    [:h2 "Welcome to Confpass.me, " (:public-profile/name identity) "!"]
    [:p "This app is your " [:strong "Companion app"] " during " (config/value :compass/site-name) "!"]
    [:p "Here you'll find the event " [:strong "schedule, workshops, live streams, and activities"] " ."]
    [:h3 "Discord"]
    [:p "We've already added you to our "
     [:a {:target "_blank"
          :href (str "https://discord.com/channels/"
                     (config/value :discord/server-id))}
      "Discord server"]
     ", where you can chat with the other attendees. You'll also
      find us there, if you need any help."]
    [:h3 "Networking"]
    [:p "Use the Contact feature while networking, you can download contact
    information of the people you connected with after the event. Make sure to
    fill out your own contact information in your profile!"]
    [:h3 "The small print"]
    [c/form {:method "POST"}
     [:section
      [:label
       [:input {:type "checkbox"
                :id "incognito"
                :name "incognito"}]
       [:span "Incognito mode, don't show my name and avatar on sessions I participate in. (optional)"]]]
     [:section
      [:label
       [:input {:type "checkbox"
                :id "accept-privacy-policy"
                :name "accept-privacy-policy"}]
       [:span "I have read and agree to the "
        [:a {:href (url-for :documents/privacy-policy) :target "_blank"}
         "Privacy Policy"] ". "
        [:span.mandatory "*"]]]]
     [:section
      [:input {:type "submit" :value "Continue"
               :cx-enabled-by "#accept-privacy-policy"}]]]]))
