(ns lambdaisland.compass.config
  "Configuration API

  Gives us just enough flexiblity to comfortably have separate prod/edn
  config+secrets, maximally checking in anything that isn't a secret into the
  repo, and allowing for convenient local overrides.

  Main public API: `config/value`, the rest is plumbing.

  Merge in order (where applicable)

  - `resources/lambdaisland/compass/config.edn` - Base config file, shared values
  - `resources/lambdaisland/compass/prod.edn`   - Prod specific values
  - `resources/lambdaisland/compass/dev.edn`    - Dev specific values
  - `config.local.edn`                       - Local overrides, does not get checked in
  - environment variables                    - After munging, e.g. :f-oo/bar -> F_OO__BAR

  When running with launchpad config files are watched for changes.
  "
  (:require
   [lambdaisland.config :as config]
   [lambdaisland.config.cli :as cli]
   [lambdaisland.config.systemd-creds :as system-creds]))

(def prefix "compass")

(defonce config
  (-> {:prefix prefix}
      config/create
      system-creds/add-provider
      cli/add-provider))

(defn value [k]
  (config/get config k))

(defn values []
  (config/values config))
