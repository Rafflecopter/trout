(ns user
  (:require
   [weasel.repl.websocket]
   [cemerick.piggieback]
   [cljs.repl :as repl]
   [cljs.repl.node :as node]))

(defn ws-repl []
  (cemerick.piggieback/cljs-repl
   (weasel.repl.websocket/repl-env :ip "0.0.0.0" :port 9009)))

(defn node-repl []
  (cemerick.piggieback/cljs-repl
   (node/repl-env)
   :output-dir ".cljs_node_repl"
   :cache-analysis true
   :source-map true))


