(ns lambdaisland.compass.routes.documents
  (:require
   [clojure.java.io :as io]
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

(o/defstyled markdown-article :article
  {:max-width "60rem" :line-height 1.6}
  [:h1 {:font-size t/--font-size-6 :margin-bottom t/--size-4}]
  [:h2 {:font-size t/--font-size-5 :margin-top t/--size-5 :margin-bottom t/--size-2}]
  [:h3 {:font-size t/--font-size-4 :margin-top t/--size-4 :margin-bottom t/--size-2 :text-decoration "underline"}]
  [:p {:margin-bottom t/--size-1}]
  [:ul {:padding-left t/--size-4 :list-style-type "circle"}]
  [:ol {:list-style-type "decimal"}]
  [:li {:margin-bottom t/--size-1 :max-inline-size "50rem"}])
;; (o/defrules privacy-policy
;;   [:article
;;    {:max-width "48rem" :line-height 1.6}
;;    [:h1 {:font-size "1.75rem" :margin-bottom "1rem"}]
;;    [:h2 {:font-size "1.25rem" :margin-top "1.5rem" :margin-bottom "0.5rem"}]
;;    [:p {:margin-bottom "0.75rem"}]
;;    [:ul {:padding-left "1.5rem" :margin-bottom "0.75rem"}]
;;    [:li {:margin-bottom "0.25rem"}]])
(defn GET-privacy-policy [_req]
  {:html/body
   [markdown-article
    (m/component
     (m/md->hiccup (slurp (io/resource "compass/privacy_policy.md"))))]})

(defn routes []
  ["/documents"
   ["/privacy-policy"
    {:name :documents/privacy-policy
     :get {:handler GET-privacy-policy}}]])

(comment
  (GET-privacy-policy nil))
