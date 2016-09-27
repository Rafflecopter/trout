(defproject trout "0.1.4"
  :description "A readable 'routes as data' routing library for clojurescript"
  :url "https://github.com/Rafflecopter/trout"
  :license {:name "Unlicense"
            :url "http://unlicense.org"
            :comments "Do what you want"}

  :repositories [["clojars" {:url "https://clojars.org/repo" :creds :gpg}]]
  :deploy-repositories [["releases" :clojars]]

  :dependencies
  [[org.clojure/clojure        "1.7.0"]
   [org.clojure/clojurescript  "1.7.170"]
   [com.cemerick/url           "0.1.1"]]

  :plugins
  [[lein-cljsbuild "1.1.1"]]

  :profiles
  {:dev {:source-paths ["dev" "src"]
         :dependencies [[com.cemerick/piggieback "0.2.1"]
                        [org.clojure/tools.nrepl "0.2.11"]
                        [lein-doo "0.1.6-SNAPSHOT"]]
         :plugins [[lein-doo "0.1.6-SNAPSHOT"]
                   [com.cemerick/austin "0.1.6"]]
         :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler {:target :nodejs
                :optimizations :none
                :output-to "target/js/dev/trout.js"
                :output-dir "target/js/dev/out"}}
    {:id "test"
     :source-paths ["src" "test"]
     :compiler {:optimizations :whitespace
                :output-to "target/js/test/test.js"
                :output-dir "target/js/test/out"
                :main trout.test-runner}}]}

  :clean-targets ^{:protect false} ["out" "target" "resources" ".cljs_node_repl"]

  :aliases {"clean-build" ["do" "clean," "cljsbuild" "once"]
            "clean-repl" ["do" "clean-build," "repl"]
            "test" ["doo" "phantom" "test" "once"]
            "clean-test" ["do" "clean," "test"]}

  :doo {:build "test"}
  )
