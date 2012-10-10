(ns gh-file-reader.core-test
  (:use clojure.test
        gh-file-reader.core)
  (:require [clojure.string :as str])
  )

(deftest normalize-path-test
  (are [x y] (= x (#'gh-file-reader.core/normalize-path y))
    "/foo.txt" "foo.txt"
    "/foo.txt" "/foo.txt"))

(deftest read-content-test
  (testing "reading file"
    (let [c (read-content "liquidz" "gh-file-reader" "test/test-files/foo")]
      (are [x y] (= x y)
        false (nil? c)
        true  (contains? c :type)
        true  (contains? c :path)
        true  (contains? c :sha)
        true  (contains? c :size)
        true  (contains? c :encoding)
        true  (contains? c :content)
        true  (contains? c :_links)
        true  (contains? c :name)
        "foo" (str/trim (str-content c)))))

  (testing "reading directory"
    (let [[a b :as contents]
          (read-content "liquidz" "gh-file-reader" "test/test-files")]
      (are [x y] (= x y)
        2       (count contents)
        "bar"   (:name a)
        "foo"   (:name b)
        true    (dir? a)
        true    (file? b)))))

(deftest file?-test
  (let [c (read-content "liquidz" "gh-file-reader" "test/test-files/foo")]
    (is (file? c))
    (is (not (dir? c)))))

(deftest dir?-test
  (let [c (first (read-content "liquidz" "gh-file-reader" "test/test-files"))]
    ;; c = baz
    (is (not (file? c)))
    (is (dir? c))))

