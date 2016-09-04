(ns conceptnet-clj.expunge
  (:require [datomic.api :as d :refer [q db]]
            [clojure.java.io :as io]
            [conceptnet-clj.loader :refer [ensure-transformed-concepts]]))
