(ns user
  (:require
   [weasel.repl :as ws-repl]
   [secretary.core :as secretary :include-macros true]))

(ws-repl/connect "ws://localhost:9009" :verbose true)
