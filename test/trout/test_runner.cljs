(ns trout.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [trout.core-test]))

(doo-tests 'trout.core-test)
