(ns gh-file-reader.core-test
  (:use clojure.test
        gh-file-reader.core)
  (:require [clojure.string  :as str]
            [clojure.java.io :as io])
  (:import [java.io FileInputStream]))

(defn same-binary? [file1 file2]
  (try
    (with-open [fis1 (FileInputStream. file1)
                fis2 (FileInputStream. file2)]
      (let [b1 (byte-array (.length file1))
            b2 (byte-array (.length file2))]
        (.read fis1 b1)
        (.read fis2 b2)

        (every? (fn [[x y]] (= x y)) (partition 2 (interleave b1 b2)))))
    (catch Exception e false)))


;;; normalize-path
(deftest normalize-path-test
  (are [x y] (= x (#'gh-file-reader.core/normalize-path y))
    "/foo.txt" "foo.txt"
    "/foo.txt" "/foo.txt"
    "/bar"     "/bar"
    "/bar"     "bar"
    "/bar"     "/bar/"))

;;; read-content
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
        "foo" (str/trim (str-content c))
        ; optional data
        "liquidz" (:owner c)
        "gh-file-reader" (:repository c))))

  (testing "reading directory"
    (let [[a b :as contents]
          (read-content "liquidz" "gh-file-reader" "test/test-files")]
      (are [x y] (= x y)
        2       (count contents)
        "bar"   (:name a)
        "foo"   (:name b)
        true    (dir? a)
        true    (file? b)
        true    (contains? a :owner)
        true    (contains? b :owner)
        true    (contains? a :repository)
        true    (contains? b :repository)))))

;;; file?
(deftest file?-test
  (let [c (read-content "liquidz" "gh-file-reader" "test/test-files/foo")]
    (is (file? c))
    (is (not (dir? c)))))

;;; dir?
(deftest dir?-test
  (let [c (first (read-content "liquidz" "gh-file-reader" "test/test-files"))]
    ;; c = baz
    (is (not (file? c)))
    (is (dir? c))))

;;; not-found?
(deftest not-found?-test
  (let [existing-file     (read-content "liquidz" "gh-file-reader" "test")
        not-existing-file (read-content "liquidz" "gh-file-reader" "notexisting")]
    (is (not (:not-found? existing-file)))
    (is (:not-found? not-existing-file))))

(deftest download-test
  (testing "download file"
    (is (download (read-content "liquidz" "gh-file-reader" "test/test-files/foo")
                  "."))
    (let [file (io/file "foo")]
      (are [x y] (= x y)
        true  (.exists file)
        "foo" (str/trim (slurp file)))
      (.delete file)))

  (testing "download directory"
    (is (download (read-content "liquidz" "gh-file-reader" "test/test-files/bar")
                  "./bar"))
    (let [bar-dir  (io/file "bar")
          baz-file (io/file "bar/baz")
          bin-dir  (io/file "bar/bin")
          ico-file (io/file "bar/bin/favicon.ico")]
      (are [x y] (= x y)
        true  (every? #(.exists %) [bar-dir baz-file bin-dir ico-file])
        true  (every? #(.isFile %) [baz-file ico-file])
        true  (every? #(.isDirectory %) [bar-dir bin-dir])
        "baz" (str/trim (slurp baz-file)))

      ; check binary
      (is (same-binary? ico-file (io/file "test/test-files/bar/bin/favicon.ico")))

      ; delete files
      (doseq [f [baz-file ico-file bin-dir bar-dir]]
        (.delete f))))

  (testing "download not-existing file"
    (is (not (download (read-content "liquidz" "gh-file-reader" "notexisting")
                       "./bar")))))
