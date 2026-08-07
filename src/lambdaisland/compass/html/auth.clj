(ns lambdaisland.compass.html.auth
  (:require
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.html.graphics :as graphics]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defstyled discord-button :a.btn
  :mb-4 :py-3 :px-4
  {:background-color "#7289da"
   :border-radius t/--radius-2
   :color t/--gray-0
   :text-align "center"
   :--_ink-shadow "none"}
  [:&:hover {:text-decoration "none"}]
  [graphics/discord {:height "2rem" :width "2rem" :--_logo-color t/--gray-0}]
  ([{:keys [next-url text]}]
   [:<>
    {:hx-boost "false"
     :href (if next-url
             (str "/oauth2/discord/redirect?redirect_url=" next-url)
             (str "/oauth2/discord/redirect"))}
    [graphics/discord]
    (or text "Continue with Discord")]))

(o/defstyled consent-submit :button.btn#discord-login-btn
  :mb-4 :py-3 :px-4
  {:background-color "#7289da"
   :border-radius t/--radius-2
   :color t/--gray-0
   :font-size "inherit"
   :font-family "inherit"
   :border "none"
   :cursor "pointer"
   :--_ink-shadow "none"}
  [:&:hover {:text-decoration "none"}]
  ["&[cx-disabled]" {:pointer-events "none" :opacity 0.5}]
  [graphics/discord {:height "2rem" :width "2rem" :--_logo-color t/--gray-0}]
  ([{:keys [text cx-enabled-by]}]
   [:<>
    {:type "submit"
     :form "consent"
     :cx-enabled-by cx-enabled-by
     :cx-disabled "disabled"}
    [graphics/discord]
    (or text "Continue with Discord")]))

(o/defstyled consent-label :label
  :flex :items-center :gap-2 :mb-3 :cursor-pointer
  {:font-size t/--font-size-3}
  [:input {:flex-shrink 0}])

(o/defstyled consent-area :div
  :flex :flex-col
  {:text-align "left"
   :margin "0 1rem 0.5rem 1rem"
   :max-width "32rem"}
  [:a {:text-decoration "underline"}])

(o/defstyled popup :div
  :flex-col :items-center
  :gap-4
  [:.top :flex :self-end :p-2]
  [:p :m-4 :mt-0]
  [:.discord-login]
  [graphics/cross {:width "3rem" :height "2.25rem" :padding "0.4rem"
                   :--_icon-color t/--text-1}]
  [:.mandatory {:color t/--red-9 :font-size t/--font-size-1 :vertical-align :super}]
  {:max-width "50rem"}
  ([next-url]
   [:<>
    [:a.top
     [graphics/cross {:class "btn close-button" :on-click "window.modal.close()"}]]
    [:p "You can authenticate using Discord to make full use of the Compass app. This will also give you access to our Discord server where you can chat with speakers and attendees."]
    [c/form {:id "consent" :action "/login/consent" :method "POST"}
     [:input {:type "hidden" :name "next" :value (or next-url "/")}]
     [consent-area
      [consent-label
       [:input {:type "checkbox"
                :id "consent-privacy"
                :name "consent-privacy"}]
       [:span "I have read and agree to the " [:a {:href (url-for :documents/privacy-policy) :target "_blank"} "Privacy Policy"] ". "
        [:span.mandatory "*"]]]
      [consent-label
       [:input {:type "checkbox"
                :id "consent-discord"
                :name "consent-discord"}]
       [:span "I understand that Compass receives my Discord username, email, and avatar. "
        [:span.mandatory "*"]]]
      [consent-label
       [:input {:type "checkbox"
                :id "consent-profile-public"
                :name "profile-public"}]
       [:span "Show my profile in attendee listings "
        [:span.mandatory "(recommended)"]]]]]
    [consent-submit {:text "Continue with Discord"
                     :cx-enabled-by "#consent-privacy,#consent-discord"}]]))
