(ns lambdaisland.compass.http.response
  (:require
   [clojure.string :as str]
   [lambdaisland.compass.html.auth :as auth-html]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [ring.util.response :as response]))

(defn redirect
  "Returns a Ring response for an HTTP 302 redirect. Status may be
  a key in redirect-status-codes or a numeric code. Defaults to 302"
  ([url]
   (redirect url nil))
  ([url {:keys [status flash push-url?]
         :or {status :found}}]
   (let [url (str (if (vector? url)
                    (str/join "/" url)
                    url))]
     (cond-> {:status  (response/redirect-status-codes status status)
              :headers {"Location" url
                        "HX-Redirect" url}
              :body    ""}
       flash
       (assoc :flash flash)))))

(defn requires-auth
  "Ring response that instructs HTMX to render the login dialog"
  [next-url]
  {:html/layout false
   :html/body [auth-html/popup next-url]
   :headers {"HX-Retarget" "#modal"
             "HX-Reswap" "innerHTML"
             "HX-Reselect" (str "." auth-html/popup)}})

(defn wrap-requires-auth
  "Middleware that shows the login dialog if the user is not logged in"
  [handler]
  (fn [{:keys [identity] :as req}]
    (if identity
      (handler req)
      (if (get-in req [:headers "hx-request"])
        (requires-auth (if (= :get (:request-method req))
                         (:uri req)
                         "/"))
        (redirect (str "/?show-login-dialog=true"))))))
