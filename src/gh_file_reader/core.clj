(ns gh-file-reader.core
  (:require
    [clojure.string            :as str]
    [clojure.java.io           :as io]
    [clojure.data.json         :as json]
    [clojure.data.codec.base64 :as base64])
  (:import [java.io FileNotFoundException
                    ByteArrayInputStream]))

(declare ^:dynamic *owner*)
(declare ^:dynamic *repository*)

(defn- normalize-path [path]
  (if path
    (str/replace
      (if (= \/ (first path)) path (str "/" path))
      #"/$" "")))

(defn read-content
  ([path]
   (read-content *owner* *repository* path))
  ([repository path]
   (read-content *owner* repository path))
  ([owner repository path]
   (try
     (let [data (->> (normalize-path path)
                  (str "https://api.github.com/repos/" owner "/" repository "/contents")
                  slurp
                  json/read-json)]
       (if (sequential? data)
         (map #(assoc % :owner owner :repository repository) data)
         (assoc data    :owner owner :repository repository)))
     (catch FileNotFoundException e
       {:not-found? true}))))

;; GitHub Content Utility

(defn file? [content]
  (= "file" (:type content)))
(defn dir? [content]
  (= "dir" (:type content)))

(defn str-content [content]
  (-> (.getBytes (:content content))
      base64/decode
      String.))

(defn download [contents download-dir]
  (let[dir-file (io/file download-dir)
       contents (if (sequential? contents) contents [contents])]
    (if (every? #(not (:not-found? %)) contents)
      (do
        ; mkdir
        (if (not (.exists dir-file)) (.mkdir dir-file))

        (every?
          #(cond
            ; downloading directory
            (dir? %)
            (let [new-dir (normalize-path (:name %))]
              (download
                (read-content (:owner %) (:repository %) (:path %))
                (str download-dir new-dir)))

            ; downloading file
            (file? %)
            (let [c  (if (contains? % :content) %
                       (read-content (:owner %) (:repository %) (:path %)))
                  byte-arr    (-> c :content (str/replace #"\n" "") .getBytes)
                  output-name (str download-dir (normalize-path (:name c)))]
              (with-open [in  (ByteArrayInputStream. byte-arr)
                          out (io/output-stream output-name)]
                (base64/decoding-transfer in out)
                true)))
          contents))
      false)))

(defmacro with-repository [owner repository & body]
  `(binding [*owner* ~owner, *repository* ~repository] ~@body))

