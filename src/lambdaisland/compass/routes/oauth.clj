(ns lambdaisland.compass.routes.oauth
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.db :as db]
   [lambdaisland.compass.html.auth :as auth-html]
   [lambdaisland.compass.http.oauth :as oauth]
   [lambdaisland.compass.http.response :as response]
   [lambdaisland.compass.model.assets :as assets]
   [lambdaisland.compass.model.user :as user]
   [lambdaisland.compass.services.discord :as discord]
   [lambdaisland.compass.util :as util]
   [datomic.api :as d]
   [io.pedestal.log :as log]
   [lambdaisland.uri :as uri]))

(defn GET-discord-redirect
  "Kick off the Discord OAuth flow, by saving the necessary OAuth state on our
  side, and then redirecting to Discord. Preserves any consent values already
  in the session (set by POST /login/consent)."
  [{:keys [query-params session]}]
  (let [state (random-uuid)]
    (-> (uri/uri oauth/discord-oauth-endpoint)
        (uri/assoc-query*
         {:client_id     (config/value :discord/client-id)
          :response_type "code"
          :redirect_uri  (str (config/value :compass/origin) "/oauth2/discord/callback")
          :scope         (str/join " " oauth/default-scopes)
          :state         state})
        str
        response/redirect
        (assoc :session (-> (or session {})
                            (assoc :oauth/state-id state
                                   :oauth/redirect-url (get query-params "redirect_url" "/")))))))

(defn POST-login-consent
  "Store consent values in the session, then redirect to the Discord OAuth flow.
  Requires both consent checkboxes to be checked."
  [{:keys [form-params] :as req}]
  (let [privacy? (= "on" (get form-params "consent-privacy"))
        discord? (= "on" (get form-params "consent-discord"))]
    (if (and privacy? discord?)
      (let [next-url (get form-params "next" "/")
            redirect-url (if (= "/" next-url)
                           "/oauth2/discord/redirect"
                           (str "/oauth2/discord/redirect?redirect_url=" next-url))]
        (-> (response/redirect redirect-url)
            (assoc :session {:oauth/consent-privacy? true
                             :oauth/consent-discord? true
                             :oauth/profile-public? (= "on" (get form-params "profile-public"))})))
      (-> (response/redirect "/?show-login-dialog=true")
          (assoc :session {})))))

(defn user-tx [user-uuid
               {:keys [access_token refresh_token expires_in] :as body}
               {:keys [id email username global_name] :as user-info}
               {:keys [profile-public? consent-privacy? consent-discord?] :as _consent}]
  #_(def user-info user-info)
  (let [existing-user (db/entity [:user/uuid user-uuid])
        avatar-id (:avatar user-info)
        discord-avatar-url (when-not (str/blank? avatar-id)
                             (str "https://cdn.discordapp.com/avatars/" id "/" avatar-id ".png"))
        avatar-url (when discord-avatar-url
                     (try
                       (assets/download-image discord-avatar-url)
                       (catch Exception e
                         (log/warn :discord/avatar-download-failed {:url discord-avatar-url}
                                   :exception e)
                         discord-avatar-url)))]
    [(cond-> {:user/uuid                 user-uuid
              :public-profile/name       (or (:public-profile/name existing-user) global_name username)
              :discord/id                id
              :discord/access-token      access_token
              :discord/refresh-token     refresh_token
              :discord/expires-at        (util/expires-in->instant expires_in)}
       avatar-url
       (assoc :public-profile/avatar-url avatar-url)
       email
       (assoc :discord/email email)
       (not existing-user)
       (assoc :public-profile/hidden? (not (boolean profile-public?)))
       consent-privacy?
       (assoc :privacy-policy/accepted-at (java.util.Date.)))]))

(defn GET-discord-callback [{:keys [query-params session]}]
  (let [{:strs [code state]}  query-params
        {:keys [status body]} (oauth/exchange-code code)]
    (cond
      (not= 200 status)
      (-> (response/redirect "/" {:flash [:p
                                          "Discord OAuth2 exchange failed."
                                          [:pre (util/pprint-str body)]]})
          (assoc :session {}))

      (not= state (str (:oauth/state-id session)))
      (-> (response/redirect "/" {:flash [:p "Discord OAuth2 invalid state."]})
          (assoc :session {}))

      (not (and (:oauth/consent-privacy? session)
                (:oauth/consent-discord? session)))
      (-> (response/redirect "/?show-login-dialog=true")
          (assoc :session {}))

      :else
      (let [{:keys [id] :as user-info} (discord/fetch-user-info (:access_token body))
            user-uuid                  (:user/uuid (d/entity (db/db) [:discord/id id]) (random-uuid))
            {:keys [status]}           (discord/join-server (:access_token body))
            consent                    {:profile-public? (:oauth/profile-public? session)
                                        :consent-privacy? (:oauth/consent-privacy? session)
                                        :consent-discord? (:oauth/consent-discord? session)}]
        @(db/transact (user-tx user-uuid body user-info consent))
        {:status  302
         :headers {"Location" (:oauth/redirect-url session "/")}
         :flash   [:p "Welcome to Compass, " (:global_name user-info) "!"
                   (case status
                     204 nil
                     201 [:br "You've also been added to "
                          [:a {:href (str "https://discord.com/channels/" (config/value :discord/server-id))}
                           "our Discord server"] "!"]
                     [:br "Unfortunately, adding you to our Discord server didn't work."])]
         :session {:identity user-uuid}}))))

(defn GET-login [req]
  {:html/layout false
   :html/body [auth-html/popup (-> req :params :next)]
   :headers {"HX-Retarget" "#modal"
             "HX-Reselect" (str "." auth-html/popup)}})

(defn routes []
  [""
   ["/oauth2"
    ["/discord"
     ["/redirect"
      {:get {:handler GET-discord-redirect}}]
     ["/callback"
      {:get {:handler GET-discord-callback}}]]]
    ["/login"
     {:name :login/index
      :get {:handler GET-login}}
     ["/consent"
      {:post {:handler POST-login-consent}}]]
   ["/logout"
    {:name :logout/index
     :get {:handler (fn [req]
                      (assoc
                        (response/redirect "/")
                        :flash "Thank you for using Compass! Please come again."
                        :session {}))}}]])
