(defproject conceptnet-clj "0.1.0-SNAPSHOT"
  :description "conceptnet-clj: Conceptnet as a graph representation in Datomic with a clojure api"
  :url "https://github.com/aivijay/conceptnet-clj"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5350"]
                 [cheshire "5.6.3"]
                 [org.slf4j/slf4j-simple "1.7.5"]
                 [org.clojure/data.json "0.2.2"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :datomic {:schemas ["resources/datomic" ["schema.edn"]]}
  :cljsbuild {:builds [{:source-paths ["src-cljs"],
                        :compiler {:pretty-printer true,
                                   :output-to "www/js/main.js",
                                   :optimizations :whitespace}}]}
  :profiles {:dev
             {:datomic {:config "resources/datomic/free-transactor-template.properties"
                        :db-uri "datomic:free://localhost:4334/conceptnet"}}})
