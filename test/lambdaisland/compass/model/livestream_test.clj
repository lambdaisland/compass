(ns lambdaisland.compass.model.livestream-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lambdaisland.compass.model.livestream :as livestream]))

(def streams
  [{:livestream/id "main" :livestream/allowed-ticket-slugs #{"streaming" "regular"}}
   {:livestream/id "workshop" :livestream/allowed-ticket-slugs #{"regular"}}])

(defn identity-with-ticket [slug]
  {:tito.ticket/_assigned-to
   [{:tito.ticket/release {:tito.release/slug slug}}]})

(deftest ticket-access
  (testing "users only receive streams allowed by their ticket release"
    (is (= ["main"]
           (mapv :livestream/id (livestream/accessible-streams
                                 (identity-with-ticket "streaming") streams))))
    (is (= ["main" "workshop"]
           (mapv :livestream/id (livestream/accessible-streams
                                 (identity-with-ticket "regular") streams)))))
  (testing "missing and unknown tickets default to no access"
    (is (empty? (livestream/accessible-streams nil streams)))
    (is (empty? (livestream/accessible-streams
                 (identity-with-ticket "donation") streams)))))
