(defproject conceptnet-clj "0.1.0-SNAPSHOT"
  :description "conceptnet-clj: Conceptnet as a graph representation in Datomic with a clojure api"
  :url "https://github.com/aivijay/conceptnet-clj"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5350"]
                 [cheshire "5.6.3"]]:datomic {:schemas ["resources/datomic" ["schema.edn"]]}
  :profiles {:dev
             {:datomic {:config "resources/datomic/free-transactor-template.properties"
                        :db-uri "datomic:free://localhost:4334/conceptnet"}}})
