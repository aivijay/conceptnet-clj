(ns conceptnet-clj.dot
  (:require [conceptnet-clj.graph :as graph]
            [conceptnet-clj.util :as u]
            [clojure.java.io :as io])
  (:gen-class))

(def ^:dynamic *colors* ["red"
                         "pink"
                         "magenta"
                         "purple"
                         "blueviolet"
                         "blue"])
