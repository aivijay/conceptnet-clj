(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.zip :as zip]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [ring.server.standalone :refer (serve)]
            [datomic.api :as d :refer (db q)]
            [conceptnet-clj.system :as sys]
            [conceptnet-clj.expunge]
            [conceptnet-clj.core :refer :all]
            [conceptnet-clj.search :refer :all]
            [cheshire.core :refer :all]
            [conceptnet-clj.system :as system]))

(defonce system nil)

(defn start-server [system]
  (let [server (serve (get-in system [:web :handler]) (:web system))]
    (assoc-in system [:web :server] server)))

(defn stop-server [system]
  (when-let [server (get-in system [:web :server])]
    (.stop server)
    (assoc-in system [:web :server] nil)))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (sys/system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system sys/start)
  (alter-var-root #'system start-server))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (when system
    (alter-var-root #'system stop-server)
    (alter-var-root #'system sys/stop)))

(defn go
    "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn reset-db []
  (when system
    (init)
    (alter-var-root #'system sys/reset-db)))

(defn import-sample-data
  "Transacts the sample data from `data/sample.jsons` into current
      system's database connection. Assumes top-level system var has an active
      database connection."
  []
  {:pre (:conn (:db system))}
  (let [conn (-> system :db :conn)
        concepts (parse-string (slurp "data/sample.jsons") true)]
    (add-cn-records conn concepts)
    :ok))