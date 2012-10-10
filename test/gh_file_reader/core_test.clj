(ns gh-file-reader.core-test
  (:use clojure.test
        gh-file-reader.core))

(deftest normalize-path-test
  (are [x y] (= x (#'gh-file-reader.core/normalize-path y))
    "/foo.txt" "foo.txt"
    "/foo.txt" "/foo.txt"))

(deftest read-content-test
  (let [c (read-content "liquidz" "misaki" "src/misaki/core.clj")]
    (are [x y] (= x y)
      false (nil? c)
      true  (contains? c :type)
      true  (contains? c :path)
      true  (contains? c :sha)
      true  (contains? c :size)
      true  (contains? c :encoding)
      true  (contains? c :content)
      true  (contains? c :_links)
      true  (contains? c :name))))

(deftest file?-test
  (is (file? {:type "file"}))
  (is (not (file? {:type "dir"}))))

(deftest dir?-test
  (is (not (dir? {:type "file"})))
  (is (dir? {:type "dir"})))

;(deftest ls-test
;  (ls "liquidz" "misaki" "/")
;
;  )
