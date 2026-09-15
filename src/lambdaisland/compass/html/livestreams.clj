(ns lambdaisland.compass.html.livestreams
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]))

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

(defn no-configured-streams []
  [:section
   [:h2 "Livestreams"]
   [:div [:p "The organizer has not yet configured any live streams."]]])

(defn no-accessible-streams [ticket-connected?]
  [:section
   [:h2 "Livestreams"]
   [:div
    (if ticket-connected?
      [:p "Your ticket does not include access to any livestreams."]
      [:p
       [:a {:href (url-for :ticket/connect)} "Connect your Ti.to ticket"]
       " to get live stream access."])]])

(o/defstyled show-page :section
  :flex-col
  {:flex 1}
  [player-frame {:flex 1} :mb-8]
  [:iframe {:height "13rem"}]
  [:nav :flex-row :mt-3 :mb-4]
  [:h2 :mb-3]
  ([{:keys [title playback-id] :as stream} playback-token streams]
   [:<>
    (when (< 1 (count streams))
      [:nav
       [c/toggle-group
        {:value (:id stream)
         :options
         (for [{:keys [title id]} streams]
           [id {:title title
                :hx-get (url-for :streams/show {:stream-id id})
                :hx-push-url (url-for :streams/show {:stream-id id})
                :hx-target (str "." show-page)
                :hx-select (str "." show-page)}])}]])
    [:h2 "Live stream: " title]
    [player-frame
     [:mux-player {"playback-id" playback-id
                   "playback-token" playback-token
                   "metadata-video-title" title}]]
    (when-let [url (get (config/value :interprefy/iframe-link) (:id stream))]
      [:<>
       [:p [:strong "ENGLISH"] "   " "For audio translation, mute the video player above, and enable audio translation below."]
       [:p [:strong "ESPAÑOL"] "   " "Para la traducción de audio, silencia el reproductor de video de arriba, y activa la traducción de audio a continuación."]
       [:p [:strong "PORTUGUÊS"] "   " "Para tradução de áudio, silencie o reprodutor de vídeo acima e ative a tradução de áudio abaixo."]
       [:iframe {:src url :scrolling "no"}]])]))

(defn stream-form
  "Create or edit form for a livestream. Pass nil to create a new stream, or an
  existing stream to prefill the form and update it with a PUT."
  [{:keys [id title mux-id playback-id allowed-ticket-slugs]}]
  [:form.form-card-styling
   {:method "post" :action (if id (str "/admin/livestreams/" id) "/admin/livestreams")}
   [:label {:for "id"}
    [:span "Stream ID (URL-safe slug, e.g. main-stage):"]
    [:input (cond-> {:type "text" :name "id" :id "id" :required true
                     :pattern "[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*"}
              id (assoc :value id :readonly true))]]
   [:label {:for "title"}
    [:span "Title:"]
    [:input (cond-> {:type "text" :name "title" :id "title" :required true}
              title (assoc :value title))]]
   [:label {:for "allowed-ticket-slugs"}
    [:span "Allowed Ti.to release slugs (comma-separated):"]
    [:input (cond-> {:type "text" :name "allowed-ticket-slugs" :id "allowed-ticket-slugs"
                     :placeholder "streaming, regular-conference"}
              allowed-ticket-slugs
              (assoc :value (str/join ", " (sort allowed-ticket-slugs))))]]
   [:label {:for "mux-stream-id"}
    [:span (if id "MUX Stream ID:" "MUX Stream ID (leave empty to create a new one):")]
    [:input (cond-> {:type "text" :name "mux-stream-id" :id "mux-stream-id"}
              mux-id (assoc :value mux-id))]]
   [:label {:for "mux-playback-id"}
    [:span (if id "MUX Playback ID:" "MUX Playback ID (leave empty to create a new one):")]
    [:input (cond-> {:type "text" :name "mux-playback-id" :id "mux-playback-id"}
              playback-id (assoc :value playback-id))]]
   (when-not id
     [:label.checkbox
      [:span "Test stream"]
      [:span
       [:input {:type "checkbox" :name "test" :value "true"}]
       [:span "Create as Mux test stream (no live-stream usage charges, 5 min limit)"]]])
   [:input {:type "submit" :value (if id "Save Livestream" "Create Live Stream")}]])

(defn edit-stream
  "Edit view for a livestream, showing the creation form prefilled with the
  stream's current values."
  ([stream] (edit-stream stream nil))
  ([stream error-message]
   [:section
    [:h2 "Edit livestream"]
    (when stream
      [:p "Stream " [:strong (:title stream)]])
    (when error-message
      [:p.error error-message])
    [stream-form stream]
    [:p [:a {:href "/admin/livestreams"} "← Back to livestreams"]]]))

(o/defstyled admin-index :section
  :flex-col :gap-4
  ([streams created-stream] (admin-index streams created-stream nil))
  ([streams created-stream error-message]
   [:<>
    [:h2 "Manage livestreams"]
    (when created-stream
      [:div
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
         [:td (str/join ", " (sort allowed-ticket-slugs))]
         [:td
          [:div
           [:a.btn {:href (str "/admin/livestreams/" id "/edit")} "Edit"]
           [:form {:method "post" :action (str "/admin/livestreams/" id "/delete")}
            [:button {:type "submit"} "Delete"]]]]])]]
    [:h3 "Create livestream"]
    [stream-form nil]]))
