(defproject puppets-game "0.0.1.0-SNAPSHOT"
  :description "Puppets games - online strategy with indirect control."
  :url "FIXME: will be available soon"
  :profiles {:dev {:dependencies [[ring-server "0.3.1"]
                                  [ring/ring-devel "1.2.2"]
                                  [ring/ring-jetty-adapter "1.2.2"]]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [fogus/ring-edn "0.2.0"]
                 [lib-noir "0.8.2"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-servlet "1.2.2"]
                 [korma "0.3.1"]
                 [metis "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc4"]
                 [com.draines/postal "1.11.1"]
                 [crypto-password "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.4"]

                 [cljs-ajax "0.2.3"]
                 [enfocus "2.0.2"]
                 [org.clojure/clojurescript "0.0-2202"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.10"]
            [cider/cider-nrepl "0.7.0-SNAPSHOT"]]
  :cljsbuild {:builds [{:source-paths ["src/puppets/client/"]
                        :compiler {:output-to "resources/public/js/main.js"}}]}
  :ring {:port 8080
         :init puppets.server.core/init
         :handler puppets.server.core/app})
