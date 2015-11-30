(defproject trout "0.1.0-SNAPSHOT"
  :description "A readable 'routes as data' routing library for clojurescript"
  :url "https://github.com/Rafflecopter/trout"
  :license {:name "Unlicense"
            :url "http://unlicense.org"
            :comments "Do what you want"}

  :dependencies
  [[org.clojure/clojure        "1.7.0"]
   [org.clojure/clojurescript  "1.7.170"]
   [com.cemerick/url           "0.1.1"]]


  :plugins
  [[lein-cljsbuild "1.1.1"]]

  :profiles
  {:dev {:source-paths ["dev/" "src/"]
         :dependencies [[com.cemerick/piggieback "0.2.1"]
                        [weasel "0.7.0"]
                        [lein-doo "0.1.6-SNAPSHOT"]]
         :plugins [[lein-doo "0.1.6-SNAPSHOT"]]
         :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :cljsbuild
  {:builds [{:id "test"
             :source-paths ["src/"]
             ;;:notify-command "TODO: run tests here"
             :compiler {:output-to "target/js/test.js"
                        :optimizations :whitespace
                        :pretty-print true}}
            {:id "node-test"
             :source-paths ["src/" "test/"]
             :compiler {:output-to "target/js/test.js"
                        :output-dir "target"
                        :main trout.test-runner
                        :target :nodejs}}]}

  :doo {:build "node-test"})
