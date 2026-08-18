(ns lambdaisland.compass.html.livestreams-test
  (:require
   [clojure.test :refer [deftest is]]
   [lambdaisland.compass.html.livestreams :as livestreams]
   [lambdaisland.compass.http.routing :as routing]
   [lambdaisland.hiccup :as hiccup]))

(deftest renders-mux-player-attributes
  (with-redefs [routing/url-for (fn [& _] "/streams")]
    (let [rendered (hiccup/render
                    [livestreams/show
                     {:title "Main Stage" :playback-id "playback-123"}
                     "signed-token"])]
      (is (re-find #"<mux-player " rendered))
      (is (re-find #"playback-id=\"playback-123\"" rendered))
      (is (re-find #"playback-token=\"signed-token\"" rendered))
      (is (re-find #"metadata-video-title=\"Main Stage\"" rendered)))))
