(ns lambdaisland.compass.services.mux-test
  (:require
   [charred.api :as charred]
   [clojure.test :refer [deftest is]]
   [lambdaisland.compass.config :as config]
   [lambdaisland.compass.services.mux :as mux])
  (:import
   (java.io StringWriter)
   (java.nio.charset StandardCharsets)
   (java.security KeyPairGenerator Signature)
   (java.security.interfaces RSAPrivateCrtKey)
   (java.time Instant)
   (java.util Base64)
   (org.bouncycastle.asn1.pkcs RSAPrivateKey)
   (org.bouncycastle.util.io.pem PemObject PemWriter)))

(defn encoded-mux-private-key [private-key]
  (let [pem (str "-----BEGIN PRIVATE KEY-----\n"
                 (.encodeToString (Base64/getMimeEncoder 64 (.getBytes "\n"))
                                  (.getEncoded private-key))
                 "\n-----END PRIVATE KEY-----\n")]
    (.encodeToString (Base64/getEncoder)
                     (.getBytes pem StandardCharsets/UTF_8))))

(defn encoded-mux-private-key-pkcs1
  "Encode an RSA private key as a base64-wrapped PKCS#1 (\"BEGIN RSA PRIVATE
  KEY\") PEM, the format Mux hands out keys in."
  [^RSAPrivateCrtKey private-key]
  (let [asn1-key (RSAPrivateKey.
                  (.getModulus private-key)
                  (.getPublicExponent private-key)
                  (.getPrivateExponent private-key)
                  (.getPrimeP private-key)
                  (.getPrimeQ private-key)
                  (.getPrimeExponentP private-key)
                  (.getPrimeExponentQ private-key)
                  (.getCrtCoefficient private-key))
        writer (StringWriter.)]
    (with-open [pem-writer (PemWriter. writer)]
      (.writeObject pem-writer (PemObject. "RSA PRIVATE KEY" (.getEncoded asn1-key))))
    (.encodeToString (Base64/getEncoder)
                     (.getBytes (.toString writer) StandardCharsets/UTF_8))))

(defn decode-segment [segment]
  (String. (.decode (Base64/getUrlDecoder) segment)
           StandardCharsets/UTF_8))

(deftest signed-playback-id
  (is (= "signed-id"
         (mux/signed-playback-id
          {:playback_ids [{:id "public-id" :policy "public"}
                          {:id "signed-id" :policy "signed"}]})))
  (is (nil? (mux/signed-playback-id {:playback_ids [{:id "public-id" :policy "public"}]}))))

(deftest create-mux-live-stream!-missing-credentials
  (with-redefs [config/value {}]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"token"
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

(deftest signed-playback-token-pkcs1
  (let [generator (doto (KeyPairGenerator/getInstance "RSA")
                    (.initialize 2048))
        key-pair (.generateKeyPair generator)
        values {:mux/signing-key-id "key-123"
                :mux/signing-private-key
                (encoded-mux-private-key-pkcs1 (.getPrivate key-pair))
                :mux/playback-token-ttl-seconds 43200}
        now (Instant/ofEpochSecond 1000)]
    (with-redefs [config/value values]
      (let [token (mux/playback-token "playback-123" now)
            [header claims signature] (.split token "\\.")
            verifier (Signature/getInstance "SHA256withRSA")]
        (is (= {"alg" "RS256" "typ" "JWT" "kid" "key-123"}
               (charred/read-json (decode-segment header))))
        (.initVerify verifier (.getPublic key-pair))
        (.update verifier (.getBytes (str header "." claims)
                                     StandardCharsets/UTF_8))
        (is (.verify verifier (.decode (Base64/getUrlDecoder) signature)))))))
