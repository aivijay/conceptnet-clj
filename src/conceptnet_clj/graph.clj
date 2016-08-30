(ns conceptnet-clj.graph
  (:require [clojure.set :as set]
            [clojure.core.reducers :as r]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [conceptnet-clj.util :as u]))

;;; {{{ Basic data

(defrecord Graph
  [neighbors data])

(def empty-graph (Graph. {} {}))

;;; }}}
;;; {{{ Querying

(defn adjacent? [g x y]
  (contains? ((:neighbors g) x) y))

(defn neighbors [g x]
  ((:neighbors g) x))

(defn subgraph [graph vertex-set]
  (let [intersects (fn [[v es]]
                     [v (set/intersection es vertex-set)])
        in-set (fn [x] (vertex-set (first x)))]
    (Graph. (into {}
                  (map intersects
                       (filter in-set (:neighbors graph))))
            (into {}
                  (filter in-set (:data graph))))))

;;; }}}
;;; {{{ Changing

(defn update-conj [s x]
  (conj (if (nil? s) #{} s) x))

(defn add
  ([g x y] (add g x y false))
  ([g x y bidirectional?]
   ((if bidirectional? #(add % y x false) identity)
     (update-in g [:neighbors x] #(update-conj % y)))))

(defn delete
  ([g x y] (delete g x y false))
  ([g x y bidirectional?]
   ((if bidirectional? #(delete % y x false) identity)
     (update-in g [:neighbors x] #(disj % y)))))

(defn merge-graphs [a b]
  (Graph. (merge-with set/union (:neighbors a) (:neighbors b))
          (merge (:data a) (:data b))))

;;; }}}
;;; {{{ Node values

(defn get-value
  ([g x] ((:data g) x))
  ([g x k] ((get-value g x) k)))

(defn set-value
  ([g x v] (assoc-in g [:data x] v))
  ([g x k v] (set-value g x (assoc (get-value g x) k v))))

(defn update-value
  ([g x f] (set-value g x (f (get-value g x))))
  ([g x k f] (set-value g x k (f (get-value g x k)))))

;;; }}}
;;; {{{ Traversal

(defn get-vertices [graph]
  (reduce set/union (set (keys (:neighbors graph)))
          (vals (:neighbors graph))))

(defn get-edges [graph]
  (let [pair-edges (fn [[v neighbors]]
                     (map #(vector v %) neighbors))]
    (mapcat pair-edges (:neighbors graph))))

(defn bf-seq
  ([get-neighbors a]
   (bf-seq
     get-neighbors
     (conj clojure.lang.PersistentQueue/EMPTY [a])
     #{a}))
  ([get-neighbors q seen]
   (lazy-seq
     (when-not (empty? q)
       (let [current (first q)
             nbors (remove seen (get-neighbors (last current)))]
         (cons current
               (bf-seq get-neighbors
                       (into (pop q)
                             (map #(conj current %) nbors))
                       (into seen nbors))))))))

(defn breadth-first [graph a]
  (bf-seq (:neighbors graph) a))

;;
;; Breadth first search
;;
(defn bfs [graph a b]
  (first (filter #(= (last %) b) (breadth-first graph a))))

;;; }}}
;;; {{{ Paths

(defn find-all-paths [graph]
  (->> graph
       get-vertices
       (mapcat #(breadth-first graph %))
       (map #(hash-map :start (first %) :dest (last %) :path %))))

(defn write-paths [path-seq filename]
  (with-open [f (io/writer filename)]
    (doseq [path path-seq]
      (.write f (json/write-str path))
      (.write f "\n"))))

(defn iter-paths [filename]
  (let [f (io/reader filename)
        lazy (fn lazy [wrapped]
               (lazy-seq
                 (if-let [s (seq wrapped)]
                   (cons (first s) (lazy (rest s)))
                   (.close f))))]
    (lazy (map #(json/read-str % :key-fn keyword) (line-seq f)))))

;;; }}}
;;; {{{ Metrics

(defn density [graph]
  (let [n (count (get-vertices graph))
        e (count (get-edges graph))]
    (/ (* 2.0 e) (* n (dec n)))))

(defn degree [graph vertex]
  (count ((:neighbors graph) vertex)))

(defn avg-degree [graph]
  (/ (* 2.0 (count (get-edges graph)))
     (count (get-vertices graph))))

(defn clustering-coeff [graph n]
  (let [cluster ((:neighbors graph) n)
        edges (filter cluster (mapcat (:neighbors graph) cluster))
        e (count edges)
        k (count cluster)]
    (if (= k 1)
      0
      (/ (* 2.0 e) (* k (dec k))))))

(defn avg-cluster-coeff [graph]
  (let [vs (vec (get-vertices graph))]
    (/ (r/fold + (r/map #(clustering-coeff graph %) vs))
       (double (count vs)))))

(defn accum-betweenness
  [{:keys [paths betweenness reachable]} [v v-paths]]
  (let [v-paths (filter #(> (count %) 1) v-paths)]
    {:paths (+ paths (count v-paths)),
     :betweenness (merge-with +
                              betweenness
                              (frequencies (flatten v-paths))),
     :reachable (assoc reachable v (count v-paths))}))

(defn ->ratio [total [k c]]
  [k (double (/ c total))])

(defn finish-betweenness
  [{:keys [paths betweenness reachable] :as metrics}]
  (assoc metrics
    :betweenness (->> betweenness
                      (map #(->ratio paths %))
                      (into {}))
    :reachable (->> reachable
                    (map #(->ratio paths %))
                    (into {}))))

(defn metrics [graph]
  (let [mzero {:paths 0, :betweenness {}, :reachable {}}]
    (->> graph
         get-vertices
         (pmap #(vector % (breadth-first graph %)))
         (reduce accum-betweenness mzero)
         finish-betweenness)))

(defn is-connected? [graph]
  (let [vs (get-vertices graph)
        v-count (count vs)]
    (loop [vs vs]
      (if-let [v (first vs)]
        (if (= v-count (count (breadth-first graph v)))
          (recur (rest vs))
          false)
        true))))

;;; }}}
;;; {{{ Degrees

(defn degrees-between [graph n from]
  (let [neighbors (:neighbors graph)]
    (loop [d [{:degree 0, :neighbors #{from}}],
           seen #{from}]
      (let [{:keys [degree neighbors]} (last d)]
        (if (= degree n)
          d
          (let [next-neighbors (->> neighbors
                                    (mapcat (:neighbors graph))
                                    (remove seen)
                                    set)]
            (recur (conj d {:degree (inc degree)
                            :neighbors next-neighbors})
                   (into seen next-neighbors))))))))

(defn store-degrees-between
  ([graph degrees]
   (let [store (fn [g {:keys [degree neighbors]}]
                 (reduce #(set-value %1 %2 degree) g neighbors))]
     (reduce store graph degrees)))
  ([graph n from]
   (store-degrees-between graph (degrees-between graph n from))))

(defn degrees-between-subgraph [graph n from]
  (let [marked (store-degrees-between graph n from)
        v-set (set (map first (filter second (:data marked))))
        sub (subgraph marked v-set)]
    {:graph marked, :subgraph sub}))

(defn get-degree-spread [degrees]
  (reduce set/union #{} (map :neighbors degrees)))

(defn get-all-degree-spread [graph n]
  (let [graph-size (count (:neighbors graph))]
    (->> graph
         :neighbors
         keys
         vec
         (r/map #(vector % (degrees-between graph n %)))
         (r/map #(vector (first %) (get-degree-spread (second %))))
         (r/map #(vector (first %) (count (second %))))
         (r/fold merge #(apply assoc %1 %2)))))

(defn avg-degree-spread [graph n]
  (let [graph-size (count (:neighbors graph))]
    (/ (->> graph
            :neighbors
            keys
            vec
            (r/map #(degrees-between graph n %))
            (r/map get-degree-spread)
            (r/map count)
            (r/map #(/ % graph-size))
            (r/fold +))
       graph-size)))

;;; }}}
