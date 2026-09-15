(ns lambdaisland.compass.routes.livestreams
  (:require
   [lambdaisland.compass.html.livestreams :as html]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.compass.model.livestream :as livestream]
   [lambdaisland.compass.model.user :as user]
   [lambdaisland.compass.services.mux :as mux]))

(defn GET-streams [{:keys [identity]}]
  (if-not (seq (mux/streams))
    {:html/body [html/no-configured-streams]}
    (if-let [stream (first (livestream/accessible-streams identity (mux/streams)))]
      (response/redirect (url-for :streams/show {:stream-id (:id stream)}))
      {:html/body [html/no-accessible-streams (boolean (user/assigned-ticket identity))]})))

(defn GET-stream [{:keys [identity path-params]}]
  (if-let [stream (mux/find-stream (:stream-id path-params))]
    (if (livestream/accessible? identity stream)
      {:headers {"Cache-Control" "private, no-store"}
       :html/body [html/show-page stream (mux/playback-token (:playback-id stream))
                   (livestream/accessible-streams identity (mux/streams))]}
      {:status 403
       :html/body [html/no-accessible-streams (boolean (user/assigned-ticket identity))]})
    {:status 404
     :html/body [:p "Livestream not found."]}))

(defn routes []
  ["/streams"
   {:middleware [[response/wrap-requires-auth]]}
   ["" {:name :streams/index
         :get {:handler #'GET-streams}}]
   ["/:stream-id" {:name :streams/show
                    :get {:handler #'GET-stream}}]])
