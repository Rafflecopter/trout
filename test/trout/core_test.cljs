(ns trout.core-test
  (:require [cljs.test :as test :refer-macros [deftest testing is are]]
            [clojure.walk :refer [postwalk]]
            [trout.core :as t]
            [trout.route :as tr]
            [cemerick.url :as url]))


(defn- un-rx [x]
  (postwalk #(if (regexp? %) (.-source %) %) x))

(defn- location [x]
  (let [a (.createElement js/document "a")]
    (set! (.-href a) x)
    a))

(deftest route-creation
  (testing "(route x) creates correct routes based on input type"
    (let [correct (tr/Route. ["user" :id "settings" :page] nil)]
      (are [x] (= correct (t/route x))
        ["user" :id "settings" :page]
        '("user" :id "settings" :page)
        "/user/:id/settings/:page"))))

(deftest route-string-parsing
  ;; TODO: add lots more assertions (convert the p-t-r test suite?)
  (testing "Basic route strings parse correctly"
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

(deftest handlers
  (testing "Calls proper handler for maps"
    (let [routes {:a (t/route "/foo/:bar"),
                  :b (t/route "/baz/:qux")}
          handlers {:a #(str "-foo" (:bar %))
                    :b #(str "-bar" (:qux %))}]
      (are [x y] (= (t/handle! routes handlers x) y)
        "/foo/123" "-foo123"
        "/baz/456" "-bar456")))
  (testing "Calls proper handler for vectors"
    (let [routes [(t/route "/foo/:bar"),
                  (t/route "/baz/:qux")]
          handlers [#(str "-foo" (:bar %))
                    #(str "-bar" (:qux %))]]
      (are [x y] (= (t/handle! routes handlers x) y)
        "/foo/123" "-foo123"
        "/baz/456" "-bar456")))
  (testing "Calls not-found callback when route not found"
    (let [routes [(t/route "/foo/:bar")]
          handlers [#(str "handled " (:bar %))]]
      (are [x y] (= (t/handle! routes handlers x #(str "NOT FOUND")) y)
        "/notreal/123" "NOT FOUND"
        "/foo/asdf" "handled asdf"))))

(deftest url+location
  (testing "Properly converts URL records into strings"
    (are [x y] (= x (t/url->str (url/url y)))
      "/#/user/123"     "http://test.com/#/user/123"
      "/#/user/123"     "http://test.com#/user/123"
      "/#/user/123"     "https://test.com#/user/123"
      "/a/b/#/user/123" "http://test.com/a/b/#/user/123"
      "/a/b/#/user/123" "http://test.com/a/b#/user/123"))
  (testing "Properly converts Location objects into strings"
    (are [x y] (= x (t/location->str (location y)))
      "/#/user/123"     "http://test.com/#/user/123"
      "/#/user/123"     "http://test.com#/user/123"
      "/#/user/123"     "https://test.com#/user/123"
      "/a/b/#/user/123" "http://test.com/a/b/#/user/123"
      "/a/b/#/user/123" "http://test.com/a/b#/user/123")))

(deftest accept-route
  (testing "(route) accepts a Route properly"
    (is (false? (instance? tr/Route (.-pathv (t/route (t/route "/a/b"))))))))

(deftest concat-routes
  (testing "concat combines routes properly"
    (is (= ["user" :id "settings" :page-id/+ "alerts" '*]
           (t/concat "user" [:id] ["settings"] "/:page-id+" ["alerts"] "/*")))))

(deftest match-maps-and-colls
  (testing "Correctly matches maps and other generic collections"
    (let [routes {:foo (t/route "/foo")
                  :user-home (t/route "/user/:user-id")
                  :org-user-home (t/route "/org/:org-id/user/:user-id")}]
      (are [x y] (= (t/match routes x) y)
        "/foo"  {}
        "/a/b"  nil
        "/user/928ajaue" {:user-id "928ajaue"}
        "/org/jeje0101ksks/user/8kk282iaia" {:org-id "jeje0101ksks" 
                                             :user-id "8kk282iaia"}))
    (let [routes [(t/route "/foo")
                  (t/route "/user/:user-id")
                  (t/route "/org/:org-id/user/:user-id")]]
      (are [x y] (= (t/match routes x) y)
        "/foo"  {}
        "/a/b"  nil
        "/user/928ajaue" {:user-id "928ajaue"}
        "/org/jeje0101ksks/user/8kk282iaia" {:org-id "jeje0101ksks" 
                                             :user-id "8kk282iaia"}))
    
    (let [routes #{(t/route "/foo")
                   (t/route "/user/:user-id")
                   (t/route "/org/:org-id/user/:user-id")}]
      (are [x y] (= (t/match routes x) y)
        "/foo"  {}
        "/a/b"  nil
        "/user/928ajaue" {:user-id "928ajaue"}
        "/org/jeje0101ksks/user/8kk282iaia" {:org-id "jeje0101ksks" 
                                             :user-id "8kk282iaia"}))))

(deftest matches?
  (testing "Calls trout.core/match"
    (is (false? (t/matches? [(t/route "/foo")] "/aaa")))))
