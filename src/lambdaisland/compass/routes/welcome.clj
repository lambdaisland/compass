(ns lambdaisland.compass.routes.welcome
  "Initial page that people get right after logging in for the first time"
  (:require
   [lambdaisland.compass.db :as db]
   [lambdaisland.compass.html.layout :as layout]
   [lambdaisland.compass.html.welcome :as welcome-html]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.http.routing :refer [url-for]]))

(defn GET-welcome [{:keys [identity] :as req}]
  {:html/layout layout/no-nav-layout
   :html/body [welcome-html/welcome-page identity]})

(defn POST-welcome [{:keys [form-params identity] :as req}]
  (if (not= "on" (get form-params "accept-privacy-policy"))
    (GET-welcome req)
    (do
      @(db/transact [{:db/id (:db/id identity) :privacy-policy/accepted-at (java.util.Date.)}])
      (response/redirect (url-for :sessions/index)))))

(defn routes []
  ["/welcome"
   {:name           :welcome/page
    :middleware     [[response/wrap-requires-auth]]
    :get            {:handler GET-welcome}
    :post           {:handler POST-welcome}}])
