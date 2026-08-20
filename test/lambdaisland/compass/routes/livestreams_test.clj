(ns lambdaisland.compass.routes.livestreams-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lambdaisland.compass.routes.livestreams :as routes]
   [lambdaisland.compass.services.mux :as mux]))

(def configured-streams
  [{:id "main-stage"
    :title "Main Stage"
    :playback-id "signed-playback"
    :allowed-ticket-slugs #{"streaming"}}])

(def eligible-identity
  {:tito.ticket/_assigned-to
   [{:tito.ticket/release {:tito.release/slug "streaming"}}]})

(def denied-identity
  {:tito.ticket/_assigned-to
   [{:tito.ticket/release {:tito.release/slug "donation"}}]})

(deftest streams-index-filters-access
  (with-redefs [mux/streams (constantly configured-streams)]
    (is (= configured-streams
           (-> (routes/GET-streams {:identity eligible-identity})
               :html/body second)))
    (is (empty? (-> (routes/GET-streams {:identity denied-identity})
                    :html/body second)))
    (is (false? (-> (routes/GET-streams {:identity {}})
                    :html/body (nth 2))))))

(deftest stream-detail-authorizes-before-signing
  (with-redefs [mux/find-stream (fn [id]
                                  (when (= "main-stage" id)
                                    (first configured-streams)))
                mux/playback-token (constantly "signed-token")]
    (testing "eligible users receive a non-cacheable player response"
      (let [response (routes/GET-stream
                      {:identity eligible-identity
                       :path-params {:stream-id "main-stage"}})]
        (is (= 200 (or (:status response) 200)))
        (is (= "private, no-store" (get-in response [:headers "Cache-Control"])))
        (is (= "signed-token" (-> response :html/body (nth 2))))))
    (testing "known but inaccessible streams are forbidden"
      (is (= 403 (:status (routes/GET-stream
                           {:identity denied-identity
                            :path-params {:stream-id "main-stage"}})))))
    (testing "unknown streams are not found"
      (is (= 404 (:status (routes/GET-stream
                           {:identity eligible-identity
                            :path-params {:stream-id "missing"}})))))))
