# conceptnet-clj

Conceptnet 5 represented as a graph database in Datomic with a clojure api to access it.

Mapping of Conceptnet 5 json data to a graph representation on top of Datomic with interface to interact with clojure.
Visualize the graphs and relations on a ClojureScript frontend as the application matures.

## Usage

Use the clojure api to interact with Conceptnet 5 as a graph from within datomic.

# Setup

1. Make sure leiningen is installed
2. `install datomic for your platform`
3. Copy .lein-env.example to .lein-env
4. Copy dev/transactor.example.properties to dev/transactor.properties
5. In another pane, run `datomic-transactor $PWD/dev/transactor.properties`
6. Start a repl with `lein repl`
7. Within the repl, run `(go)`

You can now visit http://localhost:3000 to see the web app. You also now have an
empty datomic database named conceptnet.

## Import

1. At a REPL, run the following:
```
(go)
```
```
(import-sample-data)
```

The above will import sample data into the conceptnet database.

## License

Copyright © 2016

Distributed under the Apache License version 2.0