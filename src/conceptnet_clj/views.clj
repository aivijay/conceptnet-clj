(ns conceptnet-clj.views
  (:require [hiccup.util :refer [url]]
            [clojure.string :as str]
            [conceptnet-clj.util :refer :all]
            [net.cgrand.enlive-html :as html
             :refer [defsnippet deftemplate do-> content clone-for
                     substitute set-attr nth-of-type first-of-type
                     last-of-type]]))

(defn- form-input [nth]
  [[:fieldset (nth-of-type 1)] :dl [:dd (nth-of-type nth)] :input])

(defn emit [form]
  (if (some map? form)
    (html/emit* form)
    form))

(defn render [& args]
  (apply str (flatten (map emit args))))
