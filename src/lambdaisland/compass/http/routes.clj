(ns lambdaisland.compass.http.routes
  "Combined HTTP routing table"
  (:require
   [lambdaisland.compass.routes.admin :as admin]
   [lambdaisland.compass.routes.contacts :as contacts]
   [lambdaisland.compass.routes.documents :as documents]
   [lambdaisland.compass.routes.filters :as filters]
   [lambdaisland.compass.routes.livestreams :as livestreams]
   [lambdaisland.compass.routes.meta :as meta]
   [lambdaisland.compass.routes.oauth :as oauth]
   [lambdaisland.compass.routes.profiles :as profiles]
   [lambdaisland.compass.routes.sessions :as sessions]
   [lambdaisland.compass.routes.ticket :as ticket]
   [lambdaisland.compass.routes.welcome :as welcome]))

(defn routing-table []
  [(meta/routes)
   (sessions/routes)
   (profiles/routes)
   (contacts/routes)
   (oauth/routes)
   (documents/routes)
   (filters/routes)
   (livestreams/routes)
   (ticket/routes)
   (admin/routes)
   (welcome/routes)])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
