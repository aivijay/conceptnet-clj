(ns conceptnet-clj.util
  (:require [clojure.string :as string]
            [clojure.set :as set]))

(defn mean [coll]
  (let [accum (fn [[sum size] n]
                [(+ sum n) (inc size)])
        calc-mean (fn [[sum size]] (/ sum size))]
    (calc-mean
      (reduce accum [0 0] coll))))

(defn median [coll]
  (let [n (count coll), i (quot n 2)]
    (if (zero? (mod n 2))
      (nth coll i)
      (/ (+ (nth coll i) (nth coll (inc i))) 2.0))))

(defn log
  [src & args]
  (print (str (string/join " " (concat [\[ (str (java.util.Date.)) \] src]
                                       (map str args)))
              "\n"))
  (flush))

(defn log-every
  [n m src & args]
  (when (zero? (mod n m))
    (apply log src args)))

(defn group-seq
  "This is a lazy very of group-by. Instead of returning a map, it returns a
  seq of [key [value]] pairs."
  [key-fn coll]
  (let [step (fn step [xs]
               (when-let [s (seq xs)]
                 (let [k (key-fn (first s))
                       [head-xs tail-xs] (split-with #(= k (key-fn %)) s)]
                   (cons [k head-xs] (step tail-xs)))))]
    (lazy-seq (step coll))))

(defn trim-dot
  "This removes any trailing dots from a string."
  [s]
  (if (.endsWith s ".")
    (.substring s 0 (- (.length s) 1))
    s))

(defn numbered
  [coll]
  (map vector (range) coll))

(defn arg-value
  ([args opt-key] (arg-value args opt-key nil))
  ([args opt-key default]
   (let [opt-str (str "--" (name opt-key))]
     (if-let [opt (first (filter #(.startsWith % opt-str) args))]
       (second (string/split opt #"="))
       default))))

(defn set-diff [a b]
  [(set/difference a b) (set/difference b a)])

