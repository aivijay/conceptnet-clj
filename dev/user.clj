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
            [conceptnet-clj.search :refer :all]))

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

(def data-clojure (parse-string (slurp conceptnet-clojure-file) true))

(defn import-sample-data
  "Transacts the sample data from `data/sample.jsons` into current
  system's database connection. Assumes top-level system var has an active
  database connection."
  []
  { :pre (:conn (:db system)) }
  (let [conn (-> system :db :conn)
        concepts (parse-string (slurp "data/sample.jsons") true)]
    @(d/transact conn concepts)
    :ok))