(ns clojure-lsp.parser-test
  (:require
   [clojure-lsp.parser :as parser]
   [clojure-lsp.test-helper :as h]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]
   [taoensso.timbre :as log]))

(h/reset-db-after-test)

(deftest safe-zloc-of-string
  (is (= "(ns foo) foo/bar" (z/string (z/up (#'parser/safe-zloc-of-string "(ns foo) foo/bar")))))
  (is (= "(ns foo) foo/ (+ 1 2)" (z/string (#'parser/safe-zloc-of-string "(ns foo) foo/ (+ 1 2)"))))
  (is (= "(ns foo) foo/\n(+ 1 2)" (z/string (#'parser/safe-zloc-of-string "(ns foo) foo/\n(+ 1 2)"))))
  (is (= "(ns foo) (foo/)" (z/string (#'parser/safe-zloc-of-string "(ns foo) (foo/)"))))
  (is (= "(ns foo) :\n" (z/string (#'parser/safe-zloc-of-string "(ns foo) :\n"))))
  (is (= "(ns foo) : " (z/string (#'parser/safe-zloc-of-string "(ns foo) : "))))
  (is (= "(ns foo) (:)" (z/string (#'parser/safe-zloc-of-string "(ns foo) (:)"))))
  (is (= "(ns foo)\n:\n" (z/string (#'parser/safe-zloc-of-string "(ns foo)\n:\n"))))
  (is (= "(ns foo) :foo/ (+ 1 2)" (z/string (#'parser/safe-zloc-of-string "(ns foo) :foo/ (+ 1 2)"))))
  (is (= "(ns foo) (:foo/) (+ 1 2)" (z/string (#'parser/safe-zloc-of-string "(ns foo) (:foo/) (+ 1 2)"))))
  (is (= ":user/username\n:user/\n123" (z/string (#'parser/safe-zloc-of-string ":user/username\n:user/\n123")))))

(deftest find-loc-at-pos-test
  (testing "valid code"
    (is (= nil (z/string (parser/loc-at-pos "  foo  " 1 1))))
    (is (= 'foo (z/sexpr (parser/loc-at-pos "  foo  " 1 3))))
    (is (= 'foo (z/sexpr (parser/loc-at-pos "  foo  " 1 5))))
    (is (= "  " (z/string (parser/loc-at-pos "  foo  " 1 6)))))
  (testing "invalid code"
    (is (= "foo/" (z/string (parser/loc-at-pos "(ns foo)  (foo/)  " 1 12))))
    (is (= "foo/" (z/string (parser/loc-at-pos "(ns foo)  foo/ " 1 11))))
    (is (= "foo/" (z/string (parser/loc-at-pos "(ns foo)  foo/\n" 1 11))))))

(deftest find-top-forms-test
  (let [code "(a) (b c d)"]
    (is (= '[(a) (b c d)]
           (->> {:row 1 :col 2 :end-row 1 :end-col (count code)}
                (parser/find-top-forms-in-range code)
                (map z/sexpr))))))

(deftest lein-file->edn
  (testing "simple defproject on root"
    (is (= {:foo 1
            :bar 2}
           (parser/lein-zloc->edn (z/of-string "(defproject my-project \"0.1.2\" :foo 1 :bar 2)")))))
  (testing "simple defproject with multiple code before"
    (is (= {:foo 1
            :bar 2}
           (parser/lein-zloc->edn (z/of-string (str "(def otherthing 2)\n"
                                                    "(defproject my-project \"0.1.2\" :foo 1 :bar 2)\n"
                                                    "(def something 1)")))))))
