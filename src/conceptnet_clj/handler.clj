(ns conceptnet-clj.handler
  (:require [compojure.core :refer [defroutes routes]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [conceptnet-clj.routes.home :refer [home-routes]]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn app [context]
  (-> (home-routes context)
      (routes app-routes)
      (handler/site)))

