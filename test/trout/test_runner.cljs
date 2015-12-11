(ns trout.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            trout.settings-test
            trout.route-test
            trout.core-test))

(doo-tests 'trout.route-test
           'trout.core-test
           'trout.settings-test)
