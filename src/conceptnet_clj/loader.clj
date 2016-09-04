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
            [conceptnet-clj.system :as system]
            [cheshire.core :refer :all]))

(System/setProperty "datomic.txTimeoutMsec" "30000") ;; 30 seconds

(def conn nil)
(def system (system/system))
(def ^:dynamic *batch-size* 500)

(def char-quote "\"")
(def char-tab "\t")

(defn concept-title [^String line]
  (let [tab (. line (indexOf "\t"))]
    (when (not= tab -1)
      (.. line (substring 0 tab) trim))))

(defn concept-line? [^String line]
  (and
    (not (empty? line))
    (not (.startsWith line char-quote))    ;
    (= -1 (.indexOf line "{{SUSPENDED}}")) ; Not bad data
    (= -1 (.indexOf line "(VG)"))          ;
    (= -1 (.indexOf line "V)"))))          ;

(defn load-concepts []
  (println "TODO - load-concepts"))

(defmacro ensure-transformed-file
  "in and out are bound for you"
  [[file outfile] & body]
  `(when-not (.exists (io/as-file ~outfile))
     (with-open [~'in (io/reader
                        (java.util.zip.GZIPInputStream. (io/input-stream ~file))
                        :encoding "ISO-8859-1")
                 ~'out (io/writer ~outfile)]
       ~@body)))

(defn ensure-transformed-concepts [file outfile]
  (ensure-transformed-file [file outfile]
                           (loop [[line & lines] (drop-while #(not= % "CONCEPTS LIST") (line-seq in))]
                             (when line
                               (when (concept-line? line)
                                 (when-let [title (concept-title line)]
                                   (doto out
                                     (.write title)
                                     (.newLine))))
                               (recur lines)))))

(defn -main [& args]
  (println "\nTransforming files for faster load...")
  (let [system (system/start (system/system))]
    (alter-var-root #'conn (constantly (:conn (:db system))))
    (time (do
      (println "\nLoading concepts...")
      (load-concepts)))

    (system/stop system)))