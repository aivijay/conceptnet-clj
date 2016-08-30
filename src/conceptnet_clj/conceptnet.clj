(ns conceptnet-clj.conceptnet
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.core.reducers :as r]
            [conceptnet-clj.graph :as g]
            [conceptnet-clj.util :as u]
            [me.raynes.fs :as fs])
  (:import [java.io File]))

(defn read-edge-file [filename]
  (with-open [f (io/reader filename)]
    (->>
      f
      line-seq
      (r/map #(string/split % #"\s+"))
      (r/map #(mapv (fn [x] (Long/parseLong x)) %))
      (r/reduce #(g/add %1 (first %2) (second %2)) g/empty-graph))))

(defn read-edge-files [ego-dir]
  (r/reduce g/merge-graphs {}
            (r/map read-edge-file
                   (fs/find-files ego-dir #".*\.edges$"))))

(defn json-nodes [graph]
  (map #(hash-map :n (first %1)
                  :data (g/get-value graph (first %1))
                  :index %2
                  :count (count (second %1)))
       (:neighbors graph)
       (range)))

(defn json-links [graph nodes]
  (let [index (reduce #(assoc %1 (:n %2) (:index %2)) {} nodes)]
    (loop [g (g/get-edges graph), accum [], seen #{}]
      (if-let [[src trg :as pair] (first g)]
        (if (seen pair)
          (recur (rest g) accum seen)
          (recur (rest g)
                 (conj accum {:source (index src), :target (index trg)})
                 (into seen [pair [trg src]])))
        accum))))

(defn val->vec [m]
  (map (fn [[k v]] {:id k :children (vec v)}) m))

(defn to-json
  ([graph]
   (json/write-str
     (let [nodes (json-nodes graph)]
       {:graph (val->vec (:neighbors graph)),
        :nodes nodes,
        :links (json-links graph nodes)})))
  ([graph filename]
   (with-open [f (io/writer filename)]
     (.write f (to-json graph)))))

(defn index-degrees [degrees]
  (loop [degrees (seq degrees),
         index {}]
    (if-let [{:keys [degree nodes]} (first degrees)]
      (recur (rest degrees)
             (into index (map #(vector % degree) nodes)))
      index)))

(defn degrees-to-json
  ([graph degrees]
   (let [index (index-degrees degrees)]
     (json/write-str
       {:nodes (map #(assoc % :degree (index (:n %))) (json-nodes graph)),
        :links (json-links graph)})))
  ([graph degrees filename]
   (with-open [f (io/writer filename)]
     (.write f (degrees-to-json graph degrees)))))

