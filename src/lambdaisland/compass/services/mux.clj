(ns lambdaisland.compass.services.mux
  (:require
   [charred.api :as charred]
   [clojure.string :as str]
   [lambdaisland.compass.config :as config])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security KeyFactory PrivateKey Signature)
   (java.security.spec PKCS8EncodedKeySpec)
   (java.time Instant)
   (java.util Base64)))

(def default-token-ttl-seconds (* 12 60 60))
(def stream-id-pattern #"[a-z0-9]+(?:-[a-z0-9]+)*")

(defn- valid-stream? [{:keys [id title playback-id allowed-ticket-slugs]}]
  (and (string? id)
       (re-matches stream-id-pattern id)
       (string? title)
       (not (str/blank? title))
       (string? playback-id)
       (not (str/blank? playback-id))
       (coll? allowed-ticket-slugs)
       (every? string? allowed-ticket-slugs)))

(defn validate-streams [streams]
  (let [streams (vec (or streams []))
        ids (map :id streams)]
    (when-let [invalid (first (remove valid-stream? streams))]
      (throw (ex-info "Invalid Mux livestream configuration"
                      {:stream (dissoc invalid :playback-id)})))
    (when-not (= (count ids) (count (distinct ids)))
      (throw (ex-info "Mux livestream IDs must be unique" {:ids ids})))
    (mapv #(update % :allowed-ticket-slugs set) streams)))

(defn streams []
  (validate-streams (config/value :mux/streams)))

(defn find-stream [stream-id]
  (first (filter #(= stream-id (:id %)) (streams))))

(defn- base64-url [bytes]
  (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes))

(defn- decode-signing-key [encoded-key]
  (when (str/blank? encoded-key)
    (throw (ex-info "Missing Mux signing private key" {})))
  (let [pem (String. (.decode (Base64/getDecoder) encoded-key)
                     StandardCharsets/UTF_8)
        der (-> pem
                (str/replace #"-----BEGIN PRIVATE KEY-----" "")
                (str/replace #"-----END PRIVATE KEY-----" "")
                (str/replace #"\s" "")
                ((fn [value] (.decode (Base64/getDecoder) value))))]
    (.generatePrivate (KeyFactory/getInstance "RSA")
                      (PKCS8EncodedKeySpec. der))))

(defonce ^:private signing-key-cache (atom nil))

(defn- signing-key [encoded-key]
  (let [{cached-value :encoded-key cached-key :private-key} @signing-key-cache]
    (if (= cached-value encoded-key)
      cached-key
      (let [private-key (decode-signing-key encoded-key)]
        (reset! signing-key-cache {:encoded-key encoded-key
                                   :private-key private-key})
        private-key))))

(defn- sign-rs256 [^PrivateKey private-key signing-input]
  (let [signature (Signature/getInstance "SHA256withRSA")]
    (.initSign signature private-key)
    (.update signature (.getBytes signing-input StandardCharsets/UTF_8))
    (base64-url (.sign signature))))

(defn playback-token
  ([playback-id]
   (playback-token playback-id (Instant/now)))
  ([playback-id ^Instant now]
   (let [key-id (config/value :mux/signing-key-id)
         encoded-key (config/value :mux/signing-private-key)
         ttl (or (config/value :mux/playback-token-ttl-seconds)
                 default-token-ttl-seconds)]
     (when (str/blank? key-id)
       (throw (ex-info "Missing Mux signing key ID" {})))
     (when-not (and (integer? ttl) (pos? ttl))
       (throw (ex-info "Mux playback token TTL must be a positive integer"
                       {:ttl ttl})))
     (let [header (base64-url (.getBytes
                               (charred/write-json-str
                                {"alg" "RS256" "typ" "JWT" "kid" key-id})
                               StandardCharsets/UTF_8))
           claims (base64-url (.getBytes
                               (charred/write-json-str
                                {"sub" playback-id
                                 "aud" "v"
                                 "exp" (+ (.getEpochSecond now) ttl)
                                 "kid" key-id})
                               StandardCharsets/UTF_8))
           signing-input (str header "." claims)]
       (str signing-input "."
            (sign-rs256 (signing-key encoded-key) signing-input))))))
