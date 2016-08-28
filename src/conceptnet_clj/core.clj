(ns conceptnet-clj.core
  (:require [cheshire.core :refer :all])
  (:require [clojure.pprint :as pp])
  (:require [datomic.api :as d]))

(def conceptnet-clojure-file "data/conceptnet-en-clojure.jsons")

(first (parse-string (slurp conceptnet-clojure-file) true))

(def data (parse-string (slurp conceptnet-clojure-file) true))

(pp/pprint  (take 3 data))

(keys (first data))

(def d1 (first data))
(def d2 (second data))

;; A record from conceptnet5
;;
;; (:rel :features :license :sources :start :surfaceText :source_uri :weight :id :surfaceEnd :context :surfaceStart :uri :end :dataset)
;;

;; Database connection
;; datomic:free://localhost:4334/<DB-NAME>
(def uri "datomic:free://localhost:4334/conceptnet")

(defn create-empty-db [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)
        schema (load-file "resources/datomic/schema.edn")]
    (d/transact conn schema)
    conn))

;; Create conceptnet db
(create-empty-db uri)

(def conn (d/connect uri))

;; ------------ Query functions -------------
(defn add-cn-data [cn-data]
  (let [cn-id (d/tempid :db.part/user)]
    (println (str "cn-id=" cn-id))
    (pp/pprint cn-data)
    @(d/transact conn [{:db/id cn-id
                        :cnet/rel (:rel cn-data)
                        :cnet/license (:license cn-data)
                        :cnet/start (:start cn-data)
                        :cnet/surfaceText (:surfaceText cn-data)
                        :cnet/source_uri (:source_uri cn-data)
                        :cnet/weight (:weight cn-data)
                        :cnet/id (:id cn-data)
                        :cnet/surfaceEnd (:surfaceEnd cn-data)
                        :cnet/context (:context cn-data)
                        :cnet/surfaceStart (:surfaceStart cn-data)
                        :cnet/uri (:uri cn-data)
                        :cnet/end (:end cn-data)
                        :cnet/dataset (:dataset cn-data)}])))

(add-cn-data d1)
(add-cn-data d2)

;;
;; add-cn-records - add a list of maps of conceptnet data
;;
(defn add-cn-records [records]
  (doseq [record records] (prn record) (add-cn-data record)))

(add-cn-records data)

(defn find-all-concepts []
  (d/q '[:find ?concept ?surfaceStart
         :where [?eid :cnet/surfaceText ?concept]
         [?eid :cnet/surfaceStart ?surfaceStart]]
       (d/db conn)))

(find-all-concepts)

(defn find-concepts-for-concept [concept-name]
  (d/q '[:find ?concept ?surfaceStart ?rel ?surfaceEnd
         :in $ ?concept-name
         :where (or [?eid :cnet/surfaceStart ?concept-name]
                    [?eid :cnet/surfaceEnd ?concept-name])
         [?eid :cnet/surfaceText ?concept]
         [?eid :cnet/surfaceStart ?surfaceStart]
         [?eid :cnet/surfaceEnd ?surfaceEnd]
         [?eid :cnet/rel ?rel]]
       (d/db conn)
       concept-name))

(pp/pprint (find-concepts-for-concept "Clojure"))