(ns trout.core-test
  (:require [cljs.test :as test]
            [trout.core :as t]
            [trout.route :as tr]))

(test/deftest regex-parity
  (test/testing "Equivalent regexes to path-to-regexp"
    ;; TODO: perhaps we could bring in path-to-regexp as a dependency
    ;;       and then calculate the correct regexes dynamically
    (let [correct [["/foo"        "^/foo(?:/(?=$))?$"]
                   ["/:foo"       "^/([^/]+?)(?:/(?=$))?$"]
                   ["/:foo?"      "^(?:/([^/]+?))?(?:/(?=$))?$"]
                   ["/:foo*"      "^(?:/([^/]+?(?:/[^/]+?)*))?(?:/(?=$))?$"]
                   ["/:foo+"      "^/([^/]+?(?:/[^/]+?)*)(?:/(?=$))?$"]
                   ["/:foo(\\d+)" "^/(\\d+)(?:/(?=$))?$"]
                   ["/(.*)"       "^/(.*)(?:/(?=$))?$"]
                   ["/*"          "^/(.*)(?:/(?=$))?$"]
                   ["/foo/:bar/:quest?/:star*/:plus+/:re(\\d+)/([.]+)/*/-fin"
                    "^/foo/([^/]+?)(?:/([^/]+?))?(?:/([^/]+?(?:/[^/]+?)*))?/([^/]+?(?:/[^/]+?)*)/(\\d+)/([.]+)/(.*)/-fin(?:/(?=$))?$"]]]
      (doseq [[src canonical] correct]
        (test/is (= (.-source (tr/pathv->regexp (.-pathv (t/route src))))
                    (.-source (re-pattern canonical))))))))
