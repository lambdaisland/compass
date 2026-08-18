(ns lambdaisland.compass.html.livestreams
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]
   [ring.middleware.anti-forgery :as anti-forgery]))

(o/defstyled stream-list :ul
  :grid :gap-3 :p-0
  {:list-style "none"
   :grid-template-columns "repeat(auto-fit, minmax(16rem, 1fr))"}
  [:li :bg-surface-2 :rounded-lg :p-4]
  [:a :font-size-4 :font-semibold])

(o/defstyled player-frame :div
  :w-full
  {:max-width "72rem"
   :margin "0 auto"}
  [:mux-player :w-full
   {:aspect-ratio "16 / 9"
    :display "block"
    :background-color "#030507"}])

(defn index [streams ticket-connected?]
  [:section
   [:h2 "Livestreams"]
   (if (seq streams)
     [stream-list
      (for [{:keys [id title]} streams]
        [:li {:key id}
         [:a {:href (url-for :streams/show {:stream-id id})} title]])]
     [:div
      [:p "Your ticket does not include access to any livestreams."]
      (when-not ticket-connected?
        [:p
         [:a {:href (url-for :ticket/connect)} "Connect your Ti.to ticket"]
         " to check your livestream access."])])])

(defn show [{:keys [title playback-id]} playback-token]
  [:section
   [:p [:a {:href (url-for :streams/index)} "← All livestreams"]]
   [:h2 title]
   [player-frame
    [:mux-player {"playback-id" playback-id
                  "playback-token" playback-token
                  "metadata-video-title" title}]]])

(defn forbidden []
  [:section
   [:h2 "Livestream unavailable"]
   [:p "Your ticket does not include access to this livestream."]
   [:p [:a {:href (url-for :streams/index)} "View your available livestreams"]]])

(defn admin-index
  ([streams created-stream] (admin-index streams created-stream nil))
  ([streams created-stream error-message]
  [:section
   [:h2 "Manage livestreams"]
   (when error-message
     [:p {:style "color: red;"} error-message])
   (when created-stream
     [:div {:style "border: 1px solid; padding: 1em; margin-bottom: 1em;"}
      [:p "Livestream " [:strong (:title created-stream)] " created."]
      [:p "OBS server: " [:code (:rtmps-url created-stream)]]
      [:p "OBS stream key (shown once, copy it now): " [:code (:stream-key created-stream)]]])
   [:table
    [:thead
     [:tr [:th "ID"] [:th "Title"] [:th "Playback ID"] [:th "Allowed ticket slugs"] [:th]]]
    [:tbody
     (for [{:keys [id title playback-id allowed-ticket-slugs]} streams]
       [:tr {:key id}
        [:td id]
        [:td title]
        [:td playback-id]
        [:td (str/join ", " (sort allowed-ticket-slugs))]
        [:td
         [:form {:method "post" :action (str "/admin/livestreams/" id "/delete")}
          [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
          [:button {:type "submit"} "Delete"]]]])]]
   [:h3 "Create livestream"]
   [:form {:method "post" :action "/admin/livestreams"}
    [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
    [:label {:for "id"} "Stream ID (URL-safe slug, e.g. main-stage):"] [:br]
    [:input {:type "text" :name "id" :id "id" :required true :pattern "[a-z0-9]+(-[a-z0-9]+)*"}] [:br]
    [:label {:for "title"} "Title:"] [:br]
    [:input {:type "text" :name "title" :id "title" :required true}] [:br]
    [:label {:for "allowed-ticket-slugs"} "Allowed Ti.to release slugs (comma-separated):"] [:br]
    [:input {:type "text" :name "allowed-ticket-slugs" :id "allowed-ticket-slugs" :placeholder "streaming, regular-conference"}] [:br]
    [:label
     [:input {:type "checkbox" :name "test" :value "true"}]
     " Create as Mux test stream (no live-stream usage charges, 5 min limit)"]
    [:br]
    [:input {:type "submit" :value "Create on Mux"}]]]))
