(ns lambdaisland.compass.routes.admin
  (:require [clojure.string :as str]
            [lambdaisland.compass.db :as db]
            [lambdaisland.compass.html.components :as components]
            [lambdaisland.compass.html.livestreams :as livestreams-html]
            [lambdaisland.compass.http.response :as response]
            [lambdaisland.compass.model.user :as user]
            [lambdaisland.compass.services.mux :as mux]
            [lambdaisland.compass.services.tito :as tito]
            [lambdaisland.compass.util :as util]
            [io.pedestal.log :as log]))

(defn wrap-admin-only [handler]
  (fn [req]
    (if (user/admin? (:identity req))
      (handler req)
      {:status 403
       :html/body [:p "Admin only! Make sure you have a registered crew ticket."]})))

(defn all-users []
  (map db/entity
       (db/q '[:find [?e ...]
               :where [?e :user/uuid]]
             (db/db))))

(defn GET-users [req]
  {:html/body
   [:<>
    [components/form {:method "post" :action "/admin/users/sync"}
     [:button {:type "submit"} "Refetch ticket status from Ti.to"]]
    [:pre
     (util/pprint-str (for [u (all-users)]
                        (into (if-let [t  (user/assigned-ticket u)]
                                {:tito/ticket (into {} t)}
                                {})
                              u)))]]})

(defn POST-users-sync [_req]
  (tito/sync!)
  (response/redirect
   "/admin/users"
   {:flash [:p "Ticket data refetched from Ti.to."]}))

(defn GET-livestreams [_req]
  {:html/body [livestreams-html/admin-index (mux/streams) nil]})

(defn POST-livestreams [{{:strs [id title allowed-ticket-slugs test mux-stream-id mux-playback-id]} :form-params}]
  (try
    (let [stream (mux/create-stream!
                  {:id id
                   :title title
                   :mux-stream-id mux-stream-id
                   :mux-playback-id mux-playback-id
                   :allowed-ticket-slugs (into #{}
                                               (comp (map str/trim) (remove str/blank?))
                                               (str/split (or allowed-ticket-slugs "") #","))
                   :test? (= "true" test)})]
      {:html/body [livestreams-html/admin-index (mux/streams) stream]})
    (catch clojure.lang.ExceptionInfo e
      (log/error :mux/create-stream-failed {} :exception e)
      {:status 422
       :html/body [livestreams-html/admin-index (mux/streams) nil (ex-message e)]})))

(defn POST-livestreams-delete [{{:keys [stream-id]} :path-params}]
  (mux/delete-stream! stream-id)
  (response/redirect "/admin/livestreams"
                     {:flash [:p "Livestream " stream-id " deleted."]}))

(defn POST-livestreams-update [{{:keys [stream-id]} :path-params
                                {:strs [allowed-ticket-slugs]} :form-params}]
  (try
    (mux/update-allowed-ticket-slugs!
     stream-id
     (into #{}
           (comp (map str/trim) (remove str/blank?))
           (str/split (or allowed-ticket-slugs "") #",")))
    (response/redirect "/admin/livestreams"
                       {:flash [:p "Livestream " stream-id " updated."]})
    (catch clojure.lang.ExceptionInfo e
      (log/error :mux/update-stream-failed {} :exception e)
      {:status 422
       :html/body [livestreams-html/admin-index (mux/streams) nil (ex-message e)]})))

(defn routes []
  ["/admin" {:middleware [wrap-admin-only]}
   ["/users" {:name :admin/users
              :get {:handler #'GET-users}}]
   ["/users/sync" {:post {:handler #'POST-users-sync}}]
   ["/livestreams" {:name :admin/livestreams
                     :get {:handler #'GET-livestreams}
                     :post {:handler #'POST-livestreams}}]
   ["/livestreams/:stream-id/delete" {:post {:handler #'POST-livestreams-delete}}]
   ["/livestreams/:stream-id/update" {:post {:handler #'POST-livestreams-update}}]])
