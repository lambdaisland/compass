(ns lambdaisland.compass.routes.meta)

(defn routes []
  [""
   ["/health"
    {:name :health
     :get {:handler (fn [_] {:body "OK"})}}]
   ["/fail" {:get {:handler (fn [_] (throw (ex-info "fail" {:fail 1})))}}]])
