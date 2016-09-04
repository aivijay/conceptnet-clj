(ns conceptnet-clj.loader
  "To use, download the conceptnet jsons dump from a mirror on
  http://conceptnet5.media.mit.edu/downloads/current/conceptnet5_flat_json_5.4.tar.bz2,
  and copy them (still zipped) to the data
  folder. You can then run `lein run -m conceptnet-clj.loader`"
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async :refer [chan go >! <! close!]]
            [datomic.api :as d :refer [q db]]
            [conceptnet-clj.core :refer :all]
            [conceptnet-clj.system :as system]))

(System/setProperty "datomic.txTimeoutMsec" "30000") ;; 30 seconds

(def conn nil)
(def system (system/system))
(def ^:dynamic *batch-size* 500)

(def char-quote "\"")
(def char-tab "\t")

(defn -main [& args]
  (println "\nTransforming files for faster load...")
  (let [system (system/start (system/system))]
    (alter-var-root #'conn (constantly (:conn (:db system))))
    (time (do
      (println "\nLoading concepts...")
      (load-concepts)))

    (system/stop system)))