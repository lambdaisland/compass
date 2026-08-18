(ns lambdaisland.compass.html.livestreams
  (:require
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
