(ns conceptnet-clj.core
  (:require [cheshire.core :refer :all])
  (:require [clojure.pprint :as pp])
  (:require [datomic.api :as d :refer [q db]]
            [clojure.string :refer [split join] :as str]
            [clojure.zip :as zip]
            [conceptnet-clj.util :refer :all]
            [conceptnet-clj.search :refer [bidirectional-bfs]])
  (:import datomic.Datom))

;; -------- Add conceptnet data with a single entity id ---------------------------------------------------

(defn add-cn-data [conn cn-data]
  (let [cn-id (d/tempid :db.part/user)]
    (println (str "cn-id=" cn-id))
    (pp/pprint cn-data)
    @(d/transact conn [{:db/id #db/id[:db.part/user -1000001]
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

(defn add-cn-records [conn records]
  (doseq [record records] (prn record) (add-cn-data conn record)))

;; ------------------ Needs refinement to concept net and cleanup ---------------------
(defprotocol Eid
  (e [_]))

(extend-protocol Eid
  java.lang.Long
  (e [i] i)

  datomic.Entity
  (e [ent] (:db/id ent)))

(defn qe
  "Returns the single entity returned by a query."
  [query db & args]
  (let [res (apply d/q query db args)]
    (d/entity db (only res))))

(defn find-by
  "Returns the unique entity identified by attr and val."
  [db attr val]
  (qe '[:find ?e
        :in $ ?attr ?val
        :where [?e ?attr ?val]]
      db attr val))

(defn qes
  "Returns the entities returned by a query, assuming that
   all :find results are entity ids."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv (fn [items]
               (mapv (partial d/entity db) items)))))

(defn find-all-by
  "Returns all entities possessing attr."
  [db attr]
  (qes '[:find ?e
         :in $ ?attr
         :where [?e ?attr]]
       db attr))

(defn qfs
  "Returns the first of each query result."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv first)))

(defn modes
  "Returns the set of modes."
  [coll]
  (->> (frequencies coll)
       (reduce
         (fn [[modes ct] [k v]]
           (cond
             (< v ct)  [modes ct]
             (= v ct)  [(conj modes k) ct]
             (> v ct) [#{k} v]))
         [#{} 2])
       first))



(def acted-with-rules
  '[[(acted-with ?e1 ?e2 ?path)
     [?e1 :actor/movies ?m]
     [?e2 :actor/movies ?m]
     [(!= ?e1 ?e2)]
     [(vector ?e1 ?m ?e2) ?path]]
    [(acted-with-1 ?e1 ?e2 ?path)
     (acted-with ?e1 ?e2 ?path)]
    [(acted-with-2 ?e1 ?e2 ?path)
     (acted-with ?e1 ?x ?pp)
     (acted-with ?x ?e2 ?p2)
     [(butlast ?pp) ?p1]
     [(concat ?p1 ?p2) ?path]]
    [(acted-with-3 ?e1 ?e2 ?path)
     (acted-with-2 ?e1 ?x ?pp)
     (acted-with ?x ?e2 ?p2)
     [(butlast ?pp) ?p1]
     [(concat ?p1 ?p2) ?path]]
    [(acted-with-4 ?e1 ?e2 ?path)
     (acted-with-3 ?e1 ?x ?pp)
     (acted-with ?x ?e2 ?p2)
     [(butlast ?pp) ?p1]
     [(concat ?p1 ?p2) ?path]]])

(defn actor-or-movie-name [db eid]
  (let [ent (d/entity db (e eid))]
    (or (:cnet/end ent) (:cnet/start ent))))

(defn referring-to
  "Find all entities referring to an eid as a certain attribute."
  [db eid]
  (->> (d/datoms db :vaet (e eid))
       (map :e)))

(defn eids-with-attr-val
  "Return eids with a given attribute and value."
  [db attr val]
  (->> (d/datoms db :avet attr val)
       (map :e)))

(defn eid->actor-name
  "db is database value
  name is the actor's name"
  [db eid]
  (-> (d/entity db (e eid))
      :cnet/start))

(defn actor-search
  "Returns set with exact match, if found. Otherwise query will
  be formatted with format-query passed as-is to Lucene"
  [db query]
  (if (str/blank? query)
    #{}
    (if-let [eid (d/entid db [:cnet/start query])]
      [{:name query :actor-id eid}]
      (mapv #(zipmap [:actor-id :name] %)
            (q '[:find ?e ?name
                 :in $ ?search
                 :where [(fulltext $ :cnet/start ?search) [[?e ?name]]]]
               db (format-query query))))))

(defn movie-actors
  "Given a datomic database value and a movie id,
  returns ids for actors in that movie."
  [db eid]
  (map :e (d/datoms db :vaet eid :actor/movies)))

(defn actor-movies
  "Given a datomic database value and an actor id,
  returns ids for movies that actor was in."
  [db eid]
  (map :v (d/datoms db :eavt eid :actor/movies)))

(defn immediate-connections
  "d is database value
  eid is actor's entity id"
  [db eid]
  (->> (actor-movies db eid)
       (mapcat (partial referring-to db))))

(defn neighbors
  "db is database value
  eid is an actor or movie eid"
  [db eid]
  (or (seq (actor-movies db (e eid)))
      (seq (movie-actors db (e eid)))))

(defn zipper
  "db is database value
  eid is actor's entity id"
  [db eid]
  (let [children (partial immediate-connections db)
        branch? (comp seq children)
        make-node (fn [_ c] c)]
    (zip/zipper branch? children make-node eid)))

(defn search [db start end]
  (let [s (partial actor-search db)
        starts (s start)
        ends (s end)]
    (for [p1 starts, p2 ends]
      [p1 p2])))

(defn path-at-depth [db source target depth]
  (let [rule (symbol (str "acted-with-" depth))]
    (q (concat '[:find ?path
                 :in $ % ?actor ?target
                 :where]
               [(list rule '?actor '?target '?path)])
       db acted-with-rules source target)))

(defn ascending-years? [annotated-node]
  (if-let [years (->> annotated-node
                      (map :year)
                      (filter identity)
                      seq)]
    (apply <= years)
    true))

(defn is-documentary? [entity]
  (let [genres (:movie/genre entity)]
    (and genres (contains? genres :movie.genre/documentary))))

(defn without-documentaries
  "Returns a function suitable for use with datomic.api/filter"
  [db]
  (let [movies-attr (d/entid db :actor/movies)
        has-documentaries? (fn [db ^Datom datom]
                             (and (= movies-attr (.a datom))
                                  (is-documentary? (d/entity db (.v datom)))))]
    (fn [db ^Datom datom]
      (not (or (has-documentaries? db datom)
               (is-documentary? (d/entity db (.e datom))))))))

(defn find-id-paths [db source target]
  (let [filt (without-documentaries db)
        fdb (d/filter db filt)]
    (bidirectional-bfs source target (partial neighbors fdb))))

(defn find-annotated-paths
  [db source target]
  (let [ename (partial actor-or-movie-name db)
        annotate-node (fn [node]
                        (let [ent (d/entity db node)]
                          {:type (if (:cnet/cnet ent) "actor" "movie")
                           :year (:movie/year ent)
                           :name (ename ent)
                           :entity ent}))]
    (->> (find-id-paths db source target)
         (map (partial mapv annotate-node)))))

(defn annotate-search [db search hard-mode]
  (let [[result1 result2] search
        paths (find-annotated-paths db (:actor-id result1) (:actor-id result2))
        paths (if hard-mode
                (filter ascending-years? paths)
                paths)
        total (count paths)
        bacon-number (int (/ (-> paths first count) 2))]
    {:total total
     :paths paths
     :start (:name result1)
     :end   (:name result2)
     :bacon-number bacon-number
     :hard-mode? hard-mode}))
