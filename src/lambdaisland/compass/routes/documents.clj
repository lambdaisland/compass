(ns lambdaisland.compass.routes.documents
  (:require
   [clojure.java.io :as io]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

(o/defrules privacy-policy
  [:article
   {:max-width "48rem" :line-height 1.6}
   [:h1 {:font-size "1.75rem" :margin-bottom "1rem"}]
   [:h2 {:font-size "1.25rem" :margin-top "1.5rem" :margin-bottom "0.5rem"}]
   [:p {:margin-bottom "0.75rem"}]
   [:ul {:padding-left "1.5rem" :margin-bottom "0.75rem"}]
   [:li {:margin-bottom "0.25rem"}]])

(defn GET-privacy-policy [_req]
  {:html/body
   [:article
    (m/component
     (m/md->hiccup (slurp (io/resource "compass/privacy_policy.md"))))]})

(defn routes []
  ["/documents"
   ["/privacy-policy"
    {:name :documents/privacy-policy
     :get {:handler GET-privacy-policy}}]])

(comment
  (GET-privacy-policy nil))
