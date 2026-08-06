(ns lambdaisland.compass.db.migrations
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datomic.api :as d]
   [io.pedestal.log :as log]
   [lambdaisland.compass.config :as config])
  (:import
   (java.io File)))

(def built-in
  [{:label :cap-capacity-fn
    :tx-data
    [{:db/ident :compass.fn/cap-capacity
      :db/fn (d/function {:lang "clojure"
                          :params '[db cap]
                          :code '(for [sid (datomic.api/q '[:find [?e ...]
                                                            :in $ ?cap
                                                            :where
                                                            [?e :session/capacity ?c]
                                                            [(< ?cap ?c)]]
                                                          db cap)]
                                   [:db/add sid :session/capacity cap])})}]}])

(defn from-dir [path]
  (for [f (->> path io/file file-seq (filter File/.isFile)
               (filter #(str/ends-with? (str %) ".edn")) sort)
        :let [form (try (edn/read-string (slurp f))
                        (catch Exception e
                          (log/error :migration/invalid-edn {:file (str f)}
                                     :exception e)))
              _ (when-not (:tx-data form)
                  (log/warn :tx-data/missing {:file (str f)}
                            :message "Missing :tx-data, ignoring migration file"))]
        :when (:tx-data form)]
    (if (:label form)
      form
      (assoc form :label (keyword (str/replace (File/.getName f) #"\.edn$" ""))))))

(defn all []
  (apply concat
         built-in
         (map from-dir (config/value :data-dirs))))
