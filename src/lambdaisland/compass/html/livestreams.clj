(ns lambdaisland.compass.html.livestreams
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]
   [ring.middleware.anti-forgery :as anti-forgery]
   [lambdaisland.compass.config :as config]))

(o/defstyled stream-list :ul
  :grid :gap-3 :p-0
  {:list-style "none"
   :grid-template-columns "repeat(auto-fit, minmax(16rem, 1fr))"}
  [:li :bg-surface-2 :rounded-lg :p-4]
  [:a :font-size-4 :font-semibold])

(o/defstyled player-frame :div
  :w-full
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

(o/defstyled show :section
  :flex-col
  {:flex 1}
  [player-frame {:flex 1}]
  [:iframe {:height "13rem"}]
  ([{:keys [title playback-id]} playback-token]
   [:<>
    [:p [:a {:href (url-for :streams/index)} "← All livestreams"]]
    [:h2 title]
    [player-frame
     [:mux-player {"playback-id" playback-id
                   "playback-token" playback-token
                   "metadata-video-title" title}]]
    (when-let [url (config/value :interprefy/iframe-link)]
      [:iframe {:src url :scrolling "no"}])]))

(defn forbidden []
  [:section
   [:h2 "Livestream unavailable"]
   [:p "Your ticket does not include access to this livestream."]
   [:p [:a {:href (url-for :streams/index)} "View your available livestreams"]]])

(o/defstyled admin-index :section
  :flex-col :gap-4
  ([streams created-stream] (admin-index streams created-stream nil))
  ([streams created-stream error-message]
   [:<>
    [:h2 "Manage livestreams"]
    (when error-message
      [:p {:style "color: red;"} error-message])
    (when created-stream
      [:div {:style "border: 1px solid; padding: 1em; margin-bottom: 1em;"}
       [:p "Livestream " [:strong (:title created-stream)] " created."]
       [:p "Stream URL: " [:code (:rtmps-url created-stream)]]
       (when-not (str/blank? (:stream-key created-stream))
         [:p "Stream key (shown once, copy it now): " [:code (:stream-key created-stream)]])])
    [:table
     [:thead
      [:tr [:th "ID"] [:th "Title"] [:th "Playback ID"] [:th "Allowed ticket slugs"] [:th]]]
     [:tbody
      (for [{:keys [id title playback-id allowed-ticket-slugs]} streams]
        [:tr {:key id}
         [:td id]
         [:td title]
         [:td playback-id]
         [:td
          [:form {:method "post" :action (str "/admin/livestreams/" id "/update")}
           [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
           [:input {:type "text"
                    :name "allowed-ticket-slugs"
                    :value (str/join ", " (sort allowed-ticket-slugs))
                    :placeholder "streaming, regular-conference"}]
           [:button {:type "submit"} "Save"]]]
         [:td
          [:form {:method "post" :action (str "/admin/livestreams/" id "/delete")}
           [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
           [:button {:type "submit"} "Delete"]]]])]]
    [:h3 "Create livestream"]
    [:form.form-card-styling {:method "post" :action "/admin/livestreams"}
     [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery/*anti-forgery-token*}]
     [:label {:for "id"}
      [:span "Stream ID (URL-safe slug, e.g. main-stage):"]
      [:input {:type "text" :name "id" :id "id" :required true :pattern "[a-z0-9]+(-[a-z0-9]+)*"}]]
     [:label {:for "title"}
      [:span "Title:"]
      [:input {:type "text" :name "title" :id "title" :required true}]]
     [:label {:for "allowed-ticket-slugs"}
      [:span "Allowed Ti.to release slugs (comma-separated):"]
      [:input {:type "text" :name "allowed-ticket-slugs" :id "allowed-ticket-slugs" :placeholder "streaming, regular-conference"}]]
     [:label {:for "mux-stream-id"}
      [:span "MUX Stream ID (leave empty to create a new one):"]
      [:input {:type "text" :name "mux-stream-id" :id "mux-stream-id" :placeholder ""}]]
     [:label {:for "mux-playback-id"}
      [:span "MUX Playback ID (leave empty to create a new one):"]
      [:input {:type "text" :name "mux-playback-id" :id "mux-playback-id" :placeholder ""}]]
     [:label.checkbox
      [:span "Test stream"]
      [:span
       [:input {:type "checkbox" :name "test" :value "true"}]
       [:span "Create as Mux test stream (no live-stream usage charges, 5 min limit)"]]]
     [:input {:type "submit" :value "Create on Mux"}]]]))
