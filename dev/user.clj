(ns user
  (:require [clojure.java.shell :refer [sh]]
            [cemerick.piggieback :as pb]
            [cljs.repl.node :as node]
            [cemerick.austin :as au]))

(def opts
  [:working-dir "target"])

(defn cljs-repl [env]
  (condp = env
    :phantom (pb/cljs-repl (apply au/exec-env opts))
    :node (pb/cljs-repl (apply node/repl-env opts)))
  "Starting repl...")
