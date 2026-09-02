(ns lambdaisland.compass.services.mux
  (:require
   [buddy.core.keys :as keys]
   [buddy.sign.jws :as jws]
   [charred.api :as charred]
   [clojure.string :as str]
   [hato.client :as hato]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.db :as db])
  (:import
   (java.nio.charset StandardCharsets)
   (java.time Instant)
   (java.util Base64)))

(def default-token-ttl-seconds (* 12 60 60))
(def stream-id-pattern #"[a-z0-9]+(?:-[a-z0-9]+)*")
(def live-streams-url "https://api.mux.com/video/v1/live-streams")
(def rtmps-url "rtmps://global-live.mux.com:443/app")

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

(defn- livestream->stream [{:livestream/keys [id title playback-id allowed-ticket-slugs]}]
  {:id id
   :title title
   :playback-id playback-id
   :allowed-ticket-slugs (set allowed-ticket-slugs)})

(defn streams []
  (validate-streams
   (map livestream->stream
        (db/q '[:find [(pull ?e [*]) ...]
                :where [?e :livestream/id]]
              (db/db)))))

(defn find-stream [stream-id]
  (first (filter #(= stream-id (:id %)) (streams))))

(defn- basic-authorization [token-id secret-key]
  (str "Basic "
       (.encodeToString
        (Base64/getEncoder)
        (.getBytes (str token-id ":" secret-key) StandardCharsets/UTF_8))))

(defn create-mux-live-stream!
  "Create a live stream on Mux with a signed playback policy. Returns the Mux
  API response data, including :playback_ids and :stream_key. Throws
  ex-info on failure."
  [title & {:keys [test?]}]
  (let [token-id (config/value :mux/token-id)
        secret-key (config/value :mux/secret-key)]
    (when (str/blank? token-id)
      (throw (ex-info "Missing Mux API token id (:mux/token-id)" {})))
    (when (str/blank? secret-key)
      (throw (ex-info "Missing Mux API token secret (:mux/secret-key)" {})))
    (let [response (hato/post
                    live-streams-url
                    {:headers {"Authorization" (basic-authorization token-id secret-key)
                               "Content-Type" "application/json"
                               "Accept" "application/json"}
                     :body (charred/write-json-str
                            (cond-> {:playback_policies ["signed"]
                                     :new_asset_settings {:playback_policies ["signed"]}
                                     :meta {:title title}}
                              test? (assoc :test true)))
                     :throw-exceptions? false})
          body (try (charred/read-json (:body response) :key-fn keyword)
                    (catch Exception _ nil))]
      (when-not (= 201 (:status response))
        (let [message (or (get-in body [:error :messages 0])
                          (get-in body [:error :message]))]
          (throw (ex-info (str "Mux returned HTTP " (:status response)
                               (when message (str ": " message)))
                          {:status (:status response) :body body}))))
      (:data body))))

(defn signed-playback-id [live-stream]
  (some (fn [{:keys [id policy]}]
          (when (= "signed" policy) id))
        (:playback_ids live-stream)))

(defn create-stream!
  "Create a livestream: creates the underlying Mux live stream, then stores it
  in the database. Returns a map with the stored stream plus :stream-key
  (only available at creation time, not persisted)."
  [{:keys [id title allowed-ticket-slugs test? mux-stream-id mux-playback-id]}]
  (when-not (re-matches stream-id-pattern id)
    (throw (ex-info "Stream id must be a lowercase URL-safe slug, e.g. main-stage." {:id id})))
  (when (str/blank? title)
    (throw (ex-info "Stream title must not be blank." {})))
  (when (find-stream id)
    (throw (ex-info (str "A livestream with id " id " already exists.") {:id id})))
  (let [live-stream (if (str/blank? mux-stream-id)
                      (create-mux-live-stream! title :test? test?)
                      {:id (str/trim mux-stream-id)})
        playback-id (if (str/blank? mux-playback-id)
                      (signed-playback-id live-stream)
                      (str/trim mux-playback-id))]
    (when-not playback-id
      (throw (ex-info "Mux created the stream but did not return a signed playback ID." {})))
    @(db/transact
      [{:livestream/id id
        :livestream/title title
        :livestream/mux-id (:id live-stream)
        :livestream/playback-id playback-id
        :livestream/allowed-ticket-slugs (set allowed-ticket-slugs)}])
    {:id id
     :title title
     :playback-id playback-id
     :allowed-ticket-slugs (set allowed-ticket-slugs)
     :stream-key (:stream_key live-stream)
     :rtmps-url rtmps-url}))

(defn delete-stream! [id]
  @(db/transact [[:db/retractEntity [:livestream/id id]]]))

(defn update-allowed-ticket-slugs! [id allowed-ticket-slugs]
  (let [stream (find-stream id)]
    (when-not stream
      (throw (ex-info (str "No livestream with id " id) {:id id})))
    (let [new-slugs (set allowed-ticket-slugs)
          retractions (map (fn [slug] [:db/retract [:livestream/id id] :livestream/allowed-ticket-slugs slug])
                           (:allowed-ticket-slugs stream))
          additions (map (fn [slug] [:db/add [:livestream/id id] :livestream/allowed-ticket-slugs slug])
                         new-slugs)]
      @(db/transact (into (vec retractions) additions)))))

(defn- decode-signing-key
  "Parse a base64-encoded PEM private key. Accepts both PKCS#1
  (`BEGIN RSA PRIVATE KEY`) and PKCS#8 (`BEGIN PRIVATE KEY`) forms."
  [encoded-key]
  (when (str/blank? encoded-key)
    (throw (ex-info "Missing Mux signing private key" {})))
  (let [pem (String. (.decode (Base64/getDecoder) encoded-key) StandardCharsets/UTF_8)]
    (keys/str->private-key pem)))

(defonce ^:private signing-key-cache (atom nil))

(defn- signing-key [encoded-key]
  (let [{cached-value :encoded-key cached-key :private-key} @signing-key-cache]
    (if (= cached-value encoded-key)
      cached-key
      (let [private-key (decode-signing-key encoded-key)]
        (reset! signing-key-cache {:encoded-key encoded-key
                                   :private-key private-key})
        private-key))))

(defn playback-token
  ([playback-id]
   (playback-token playback-id (Instant/now)))
  ([playback-id ^Instant now]
   (let [key-id      (config/value :mux/signing-key-id)
         encoded-key (config/value :mux/signing-private-key)
         ttl         (or (config/value :mux/playback-token-ttl-seconds)
                         default-token-ttl-seconds)]
     (when (str/blank? key-id)
       (throw (ex-info "Missing Mux signing key ID" {})))
     (when-not (and (integer? ttl) (pos? ttl))
       (throw (ex-info "Mux playback token TTL must be a positive integer"
                       {:ttl ttl})))
     (let [claims (charred/write-json-str
                   {"sub" playback-id
                    "aud" "v"
                    "exp" (+ (.getEpochSecond now) ttl)
                    "kid" key-id})]
       (jws/sign claims (signing-key encoded-key)
                 {:alg :rs256 :header {:kid key-id :typ "JWT"}})))))
