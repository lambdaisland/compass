(ns lambdaisland.compass.services.mux-test
  (:require
   [charred.api :as charred]
   [clojure.test :refer [deftest is]]
   [hato.client :as hato]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.services.mux :as mux])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security KeyPairGenerator Signature)
   (java.time Instant)
   (java.util Base64)))

(defn encoded-mux-private-key [private-key]
  (let [pem (str "-----BEGIN PRIVATE KEY-----\n"
                 (.encodeToString (Base64/getMimeEncoder 64 (.getBytes "\n"))
                                  (.getEncoded private-key))
                 "\n-----END PRIVATE KEY-----\n")]
    (.encodeToString (Base64/getEncoder)
                     (.getBytes pem StandardCharsets/UTF_8))))

(defn decode-segment [segment]
  (String. (.decode (Base64/getUrlDecoder) segment)
           StandardCharsets/UTF_8))

(deftest stream-validation
  (is (= [{:id "main-stage"
           :title "Main Stage"
           :playback-id "playback"
           :allowed-ticket-slugs #{"streaming"}}]
         (mux/validate-streams
          [{:id "main-stage"
            :title "Main Stage"
            :playback-id "playback"
            :allowed-ticket-slugs ["streaming"]}])))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unique"
                        (mux/validate-streams
                         [{:id "main" :title "One" :playback-id "one"
                           :allowed-ticket-slugs []}
                          {:id "main" :title "Two" :playback-id "two"
                           :allowed-ticket-slugs []}])))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid"
                        (mux/validate-streams
                         [{:id "Not URL safe" :title "Title"
                           :playback-id "playback"
                           :allowed-ticket-slugs []}]))))

(deftest signed-playback-id
  (is (= "signed-id"
         (mux/signed-playback-id
          {:playback_ids [{:id "public-id" :policy "public"}
                          {:id "signed-id" :policy "signed"}]})))
  (is (nil? (mux/signed-playback-id {:playback_ids [{:id "public-id" :policy "public"}]}))))

(deftest create-mux-live-stream!-success
  (with-redefs [config/value {:mux/token-id "id" :mux/token-secret "secret"}
                hato/post (fn [url _opts]
                           (is (= mux/live-streams-url url))
                           {:status 201
                            :body (charred/write-json-str
                                   {:data {:id "live-stream-id"
                                          :stream_key "stream-key"
                                          :playback_ids [{:id "signed-id" :policy "signed"}]}})})]
    (let [live-stream (mux/create-mux-live-stream! "Main Stage")]
      (is (= "live-stream-id" (:id live-stream)))
      (is (= "signed-id" (mux/signed-playback-id live-stream))))))

(deftest create-mux-live-stream!-missing-credentials
  (with-redefs [config/value {}]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"token"
                          (mux/create-mux-live-stream! "Main Stage")))))

(deftest create-mux-live-stream!-api-error
  (with-redefs [config/value {:mux/token-id "id" :mux/token-secret "secret"}
                hato/post (fn [_url _opts]
                           {:status 422
                            :body (charred/write-json-str
                                   {:error {:messages ["Live streams are unavailable on the free plan"]}})})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"free plan"
                          (mux/create-mux-live-stream! "Main Stage")))))

(deftest signed-playback-token
  (let [generator (doto (KeyPairGenerator/getInstance "RSA")
                    (.initialize 2048))
        key-pair (.generateKeyPair generator)
        values {:mux/signing-key-id "key-123"
                :mux/signing-private-key
                (encoded-mux-private-key (.getPrivate key-pair))
                :mux/playback-token-ttl-seconds 43200}
        now (Instant/ofEpochSecond 1000)]
    (with-redefs [config/value values]
      (let [token (mux/playback-token "playback-123" now)
            [header claims signature] (.split token "\\.")
            verifier (Signature/getInstance "SHA256withRSA")]
        (is (= {"alg" "RS256" "typ" "JWT" "kid" "key-123"}
               (charred/read-json (decode-segment header))))
        (is (= {"sub" "playback-123" "aud" "v" "exp" 44200
                "kid" "key-123"}
               (charred/read-json (decode-segment claims))))
        (.initVerify verifier (.getPublic key-pair))
        (.update verifier (.getBytes (str header "." claims)
                                     StandardCharsets/UTF_8))
        (is (.verify verifier (.decode (Base64/getUrlDecoder) signature)))))))
