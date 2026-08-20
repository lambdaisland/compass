(ns lambdaisland.compass.html.welcome
  (:require
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]))


(o/defstyled welcome-page :div
  :flex :flex-col :gap-4

  ([identity]
   [:<>
    [:h2 "Welcome, " (:public-profile/name identity) "!"]
    [c/form {:method "POST"}
     [:section
      [:label [:input {:type "checkbox"
                       :id "accept-privacy-policy"
                       :name "accept-privacy-policy"}]
       [:span "I have read and agree to the "
        [:a {:href (url-for :documents/privacy-policy) :target "_blank"}
         "Privacy Policy"] ". "
        [:span.mandatory "*"]]]]
     [:section
      [:input {:type "submit" :value "Continue"
               :cx-enabled-by "#accept-privacy-policy"}]]]]))
