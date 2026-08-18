(ns lambdaisland.compass.routes.livestreams
  (:require
   [lambdaisland.compass.html.livestreams :as html]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.model.livestream :as livestream]
   [lambdaisland.compass.model.user :as user]
   [lambdaisland.compass.services.mux :as mux]))

(defn GET-streams [{:keys [identity]}]
  {:html/body [html/index
               (livestream/accessible-streams identity (mux/streams))
               (boolean (user/assigned-ticket identity))]})

(defn GET-stream [{:keys [identity path-params]}]
  (if-let [stream (mux/find-stream (:stream-id path-params))]
    (if (livestream/accessible? identity stream)
      {:headers {"Cache-Control" "private, no-store"}
       :html/body [html/show stream (mux/playback-token (:playback-id stream))]}
      {:status 403
       :html/body [html/forbidden]})
    {:status 404
     :html/body [:p "Livestream not found."]}))

(defn routes []
  ["/streams"
   {:middleware [[response/wrap-requires-auth]]}
   ["" {:name :streams/index
         :get {:handler #'GET-streams}}]
   ["/:stream-id" {:name :streams/show
                    :get {:handler #'GET-stream}}]])
