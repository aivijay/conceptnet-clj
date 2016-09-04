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
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [commons-codec "1.10"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [environ "1.0.0"]
                 [lib-noir  "0.9.9" :exclusions  [compojure clout com.fasterxml.jackson.core/jackson-core ring org.clojure/tools.reader org.clojure/core.cache]]
                 [ring-server "0.4.0"]
                 [ring "1.4.0"]
                 [clj-time  "0.11.0"]
                 [compojure "1.4.0"]]
  :plugins [[lein-ring "0.9.6" :exclusions [org.clojure/clojure]]
            [lein-beanstalk "0.2.7" :exclusions [commons-codec org.clojure/clojure]]]
  :ring {:handler conceptnet-clj.system/handler
         :init conceptnet-clj.system/init
         :destroy conceptnet-clj.system/destroy }
  :datomic {:schemas ["resources/datomic" ["schema.edn"]]}
  :cljsbuild {:builds [{:source-paths ["src-cljs"],
                        :compiler {:pretty-printer true,
                                   :output-to "www/js/main.js",
                                   :optimizations :whitespace}}]}
  :profiles {:dev
             {:source-paths ["dev" "src"]
              :dependencies [[com.datomic/datomic-free "0.9.5350" :exclusions [joda-time]]
                             [org.clojure/tools.namespace "0.2.11"]
                             [org.clojure/java.classpath "0.2.2"]
                             [javax.servlet/servlet-api "2.5"]
                             [ring-mock "0.1.5"]]
              :datomic {:config "resources/datomic/free-transactor-template.properties"
                        :db-uri "datomic:free://localhost:4334/conceptnet"}}})
