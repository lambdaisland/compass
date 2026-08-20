(ns lambdaisland.compass.html.layout
  (:require
   [charred.api :as charred]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.compass.html.auth :as auth]
   [lambdaisland.compass.html.navigation :as nav]
   [lambdaisland.ornament :as o]
   [ring.middleware.anti-forgery :as anti-forgery]))

(require 'lambdaisland.compass.css.components)
(def start-time (System/currentTimeMillis))

(o/defrules layout
  [:body
   {:max-width "100vw"}
   [:#app {:max-width "80rem" :margin "0 auto"}
    [:>main :px-2 :py-3]]])

(o/defstyled flash-box :div
  :my-3
  :px-3 :py-2
  {:background-color t/--green-1
   :color            t/--blue-12
   :border-radius    t/--radius-2
   :border-color     t/--green-2
   :border-width     "1px"
   :font-weight      "600"
   :opacity          0.5
   :animation        "fade-to-pale linear 0.5s forwards"})

(o/defrules fade-flash-box
  (garden.stylesheet/at-keyframes
   :fade-to-pale
   [:to {:opacity 1}]))

(defn base-layout* [{:keys [head body flash user request] :as opts}]
  [:html
   [:head
    [:title (config/value :compass/page-title)]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "/fonts/open-sans/open-sans.css"}]
    [:link {:rel "stylesheet" :href "/css/open-props.min.css"}]
    [:link {:rel "stylesheet" :href "/css/open-props-normalize.min.css"}]
    [:link {:rel "stylesheet" :href "/css/buttons.css"}]
    [:link {:rel "stylesheet" :href (str "/css/styles.css?t=" start-time)}]
    [:link {:rel "icon" :href "/favicon_v1.ico"}]

    [:script {:src "/js/htmx-1.9.12.js"}]
    [:script {:defer true
              :src "https://cdn.jsdelivr.net/npm/@mux/mux-player@3.13.2"}]
    [:script {:src "/js/cx.js"}]
    (when (config/value :live.js?)
      [:script {:src "/js/live.js#css"}])
    head]
   [:body
    (if (get-in request [:query-params "show-login-dialog"])
      [:dialog#modal {:style {:z-index 1}} [auth/popup "/"]]
      [:dialog#modal {} "keepme"])
    [:div#app
     {;; Have HTMX handle normal links
      :hx-boost true
      ;; Only replace what's in <main>, the navbar/menu don't get replaced
      :hx-select "main"
      :hx-target "main"

      :hx-disinherit "hx-target hx-select"
      ;; CSRF
      :hx-headers (charred/write-json-str {"x-csrf-token" anti-forgery/*anti-forgery-token*})}
     body]]])

(defn base-layout [{:keys [body user flash] :as opts}]
  [base-layout*
   (assoc opts :body
          [:<>
           [nav/menu-panel user]
           [:main
            [nav/nav-bar user]
            (when flash
              [flash-box flash])
            body]])])

(defn no-nav-layout [{:keys [body user flash] :as opts}]
  [base-layout*
   (assoc opts :body
          [:<>
           [:main
            [nav/site-name-no-nav]
            (when flash
              [flash-box flash])
            body]])])
