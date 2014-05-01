(defproject puppets-game "0.0.1.0-SNAPSHOT"
  :description "Puppets games - online strategy with indirect control."
  :url "FIXME: will be available soon"
  :profiles {:dev {:dependencies [[ring-server "0.3.1"]
                                  [ring/ring-devel "1.2.2"]
                                  [ring/ring-jetty-adapter "1.2.2"]]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [compojure "1.1.6"]
                 [enfocus "2.0.2"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-servlet "1.2.2"]
                 [korma "0.3.1"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.10"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "resources/public/js/main.js"}}]}
  :ring {:handler puppets.server.core/app})
