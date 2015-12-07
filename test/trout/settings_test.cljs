(ns trout.settings-test
  (:require [cljs.test :as test :refer-macros [deftest testing is are]]
            [trout.core :as t]
            [trout.route :as tr]
            [trout.settings :as cfg]
            ))

(deftest set-path-separator
  (testing "Setting path separator affects string parsing"
    (are [x y] (binding [cfg/*path-separator* x]
                 (= (.-pathv (t/route y)) ["foo" :bar "baz" :qux]))
      "|"  "|foo|:bar|baz|:qux"
      "@"  "@foo@:bar@baz@:qux"

      ;; this doesn't work right now
      ;; ":"  ":foo::bar:baz::qux
      ))
  (testing "Setting path separator affects string generation"
    (are [x y] (binding [cfg/*path-separator* x]
                 (= y (t/->str (t/route ["foo" :bar "baz" :qux])
                               {:bar "bbb" :qux "qqq"})))
      "|"  "|foo|bbb|baz|qqq"
      "@"  "@foo@bbb@baz@qqq")))

(deftest set-trailing-slashes
  (testing "[Dis-]Allowing trailing slashes affets matching"
    (binding [cfg/*allow-trailing-slashes* true]
      (is (t/matches? (t/route "/user/:id") "/user/123"))
      (is (t/matches? (t/route "/user/:id") "/user/123/")))
    (binding [cfg/*allow-trailing-slashes* false]
      (is (t/matches? (t/route "/user/:id") "/user/123"))
      (is (not (t/matches? (t/route "/user/:id") "/user/123/"))))))

(deftest set-navigator
  (testing "Setting navigator affects navigate!"
    (binding [cfg/*navigator* #(str "foo" "bar")]
      (is (= (t/navigate! (t/route "/foo")) "foobar")))))

(deftest set-prefix
  (testing "Changing prefix affects input/output"
    (binding [cfg/*prefix* "#"]
      (let [route (t/route "/user/:id")]
        (is (true? (t/matches? route "/#/user/123")))
        (is (= (t/->str route {:id "123"}) "/#/user/123"))))))
