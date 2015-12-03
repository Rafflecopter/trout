(ns trout.route-test
  (:require [cljs.test :as test :refer-macros [deftest testing is are]]
            [trout.core :as t]
            [trout.route :as tr]))

(deftest regex-parity
  (testing "Produces equivalent regexes to path-to-regexp"
    ;; TODO: perhaps we could bring in path-to-regexp as a dependency
    ;;       and then calculate the correct regexes dynamically
    (let [correct [[["foo"]           "^/foo(?:/(?=$))?$"]
                   [[:foo]            "^/([^/]+?)(?:/(?=$))?$"]
                   [[:foo/?]          "^(?:/([^/]+?))?(?:/(?=$))?$"]
                   [[:foo/*]          "^(?:/([^/]+?(?:/[^/]+?)*))?(?:/(?=$))?$"]
                   [[:foo/+]          "^/([^/]+?(?:/[^/]+?)*)(?:/(?=$))?$"]
                   [[[:foo #"(\d+)"]] "^/(\\d+)(?:/(?=$))?$"]
                   [[#"(.*)"]         "^/(.*)(?:/(?=$))?$"]
                   [['*]              "^/(.*)(?:/(?=$))?$"]
                   [["foo" :bar :quest/? :star/* :plus/+ [:re #"(\d+)"] #"([.]+)" '* "-fin"]
                    "^/foo/([^/]+?)(?:/([^/]+?))?(?:/([^/]+?(?:/[^/]+?)*))?/([^/]+?(?:/[^/]+?)*)/(\\d+)/([.]+)/(.*)/-fin(?:/(?=$))?$"]]]
      (doseq [[src canonical] correct]
        (is (= (.-source (tr/pathv->regexp src))
               (.-source (re-pattern canonical))))))))

(deftest params
  (testing "Correctly extracts named & indexed parameters from a path"
    (let [route (tr/Route. [:foo] nil)]
      (are [x y] (= (tr/match route x) y)
        "/test" {:foo "test"}
        "/a/b"  nil))
    (let [route (tr/Route. [:foo/?] nil)]
      (are [x y] (= (tr/match route x) y)
        "/test" {:foo "test"}
        ""      {:foo nil}
        "/a/b"  nil))
    (let [route (tr/Route. [:foo/*] nil)]
      (are [x y] (= (tr/match route x) y)
        "/test"  {:foo "test"}
        "/a/b/c" {:foo "a/b/c"}
        ""       {:foo nil}))
    (let [route (tr/Route. [:foo/+] nil)]
      (are [x y] (= (tr/match route x) y)
        "/test"  {:foo "test"}
        "/a/b/c" {:foo "a/b/c"}
        ""       nil))
    (let [route (tr/Route. [[:foo #"(\d+)"]] nil)]
      (are [x y] (= (tr/match route x) y)
        "/123"   {:foo "123"}
        "/123/4" nil
        "/abc"   nil
        "/a/b/c" nil))
    (let [route (tr/Route. [#"(\d+)"] nil)]
      (are [x y] (= (tr/match route x) y)
        "/123"   {0 "123"}
        "/123/4" nil
        "/abc"   nil
        "/a/b/c" nil))
    (let [route (tr/Route. ['*] nil)]
      (are [x y] (= (tr/match route x) y)
        "/test"  {0 "test"}
        "/a/b/c" {0 "a/b/c"}
        ""       nil))))

(deftest string-generation
  (testing "Correctly generates path strings from route + arguments"
    (are [x y z] (= x (tr/path-str (tr/Route. y nil) z))
      "/foo/123"   ["foo" :id]            {:id 123}
      "/foo/123"   ["foo" :id/?]          {:id 123}
      "/foo/"      ["foo" :id/?]          {}
      "/foo/1/2/3" ["foo" :id/*]          {:id [1 2 3]}
      "/foo/1/2/3" ["foo" :id/+]          {:id [1 2 3]}
      "/foo/123"   ["foo" [:id #"(\d+)"]] {:id 123}
      "/foo/123"   ["foo" [:id #"(\d+)"]] {:id "123"}
      "/foo/123"   ["foo" #"(\d+)"]       {0 123}
      "/foo/123"   ["foo" '*]             {0 123}
      ))
  (testing "Correctly throws when generating strings with invalid arguments"
    (are [x y] (thrown? js/TypeError (tr/path-str (tr/Route. x nil) y))
      [:id] {:id [1 2 3]} ; expected not to repeat
      [:id] {}            ; expected to be defined
      [:id/+] {:id []}    ; expected to not be empty
      [#"foo"] {0 "faa"}  ; expected to match
      )))
