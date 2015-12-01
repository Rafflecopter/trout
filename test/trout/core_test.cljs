(ns trout.core-test
  (:require [cljs.test :as test :refer-macros [deftest testing is are]]
            [clojure.walk :refer [postwalk]]
            [trout.core :as t]
            [trout.route :as tr]))


(defn- un-rx [x]
  (postwalk #(if (regexp? %) (.-source %) %) x))


(deftest route-creation
  (testing "(route x) creates correct routes based on input type"
    (let [correct (tr/Route. ["user" :id "settings" :page] nil)]
      (are [x] (= correct (t/route x))
        ["user" :id "settings" :page]
        '("user" :id "settings" :page)
        "/user/:id/settings/:page"))))

(deftest route-string-parsing
  (testing "Basic route strings parse correctly"
    ;; TODO: add lots more assertions (convert the p-t-r test suite?)
    (are [x y] (= (un-rx (t/str->pathv x)) (un-rx y))
      "/foo"            ["foo"]
      "/foo/:bar"       ["foo" :bar]
      "/foo/:bar?"      ["foo" :bar/?]
      "/foo/:bar*"      ["foo" :bar/*]
      "/foo/:bar+"      ["foo" :bar/+]
      "/foo/:bar(\\d+)" ["foo" [:bar #"(\d+)"]]
      "/foo/(.*)"       ["foo" #"(.*)"]
      "/foo/*"          ["foo" '*]))

  (testing "Composite route strings parse properly"
    (are [x y] (= (un-rx (t/str->pathv x)) (un-rx y))
      "/foo/:bar/:quest?/:star*/:plus+/:re(\\d+)/([.]+)/*/-fin"
      ["foo" :bar :quest/? :star/* :plus/+ [:re #"(\d+)"] #"([.]+)" '* "-fin"])))
