(ns trout.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [trout.route-test]))

(doo-tests 'trout.route-test)
