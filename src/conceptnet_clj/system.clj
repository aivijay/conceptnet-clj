(ns conceptnet-clj.system
  (:require
    [conceptnet-clj.graph :as graph]
    [conceptnet-clj.conceptnet :as conceptnet]
    [conceptnet-clj.dot :as dot]
    [conceptnet-clj.util :as u]
    [clojure.java.io :as io]))

(def ^:dynamic *conceptnet-dir* "./data")

(defn system []
  {:db-config [:local false nil]
   :ego-dir *conceptnet-dir*
   :graph-opened false
   :graph nil})

(defn start
  ([system] (start system false))
  ([system load-conceptnet]
   (u/log :system :start)
   (let [edges (conceptnet/read-edge-files (:conceptnet-dir system))]
     (assoc system
       :graph-opened true
       :graph edges))))

(defn stop [system]
  (u/log :system :stop)
  (assoc system :graph-opened false))

(defn read-n [n system]
  (let [read-lines (fn [file-name]
                     (with-open [f (io/reader file-name)]
                       (doall (line-seq f))))
        conceptnet-dir (str (:conceptnet-dir system) "/")]
    (assoc system
      (keyword (str "conceptnet-" n))
      {:circles (read-lines (str conceptnet-dir n ".circles"))
       :graph (read-lines (str conceptnet-dir n ".edges"))
       :conceptnet-feat (read-lines (str conceptnet-dir n ".conceptnetfeat"))
       :conceptnet (read-lines (str conceptnet-dir n ".conceptnet"))
       :concept-names (read-lines (str conceptnet-dir n ".conceptnetnames"))})))
