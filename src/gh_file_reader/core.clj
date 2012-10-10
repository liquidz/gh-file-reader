(ns gh-file-reader.core
  (:require
    [clojure.data.json :as json]
    [clojure.data.codec.base64 :as base64]
    )
  )

(declare ^:dynamic *owner*)
(declare ^:dynamic *repository*)

(defn- normalize-path [path]
  (if (= \/ (first path))
    path (str "/" path)))

(defn read-content
  ([path]
   (read-content *owner* *repository* path))
  ([repository path]
   (read-content *owner* repository path))
  ([owner repository path]
   (-> (str "https://api.github.com/repos/"
            owner "/" repository "/contents" (normalize-path path))
     slurp
     json/read-json)))

;; GitHub Content Utility

(defn file? [content]
  (= "file" (:type content)))
(defn dir? [content]
  (= "dir" (:type content)))

(defn str-content [content]
  (-> (.getBytes (:content content))
      base64/decode
      String.))

(defmacro with-repository [owner repository & body]
  `(binding [*owner* ~owner, *repository* ~repository] ~@body))

(defn -main
  "I don't do a whole lot."
  [& args]
  (with-repository "liquidz" "misaki"
    (println (read-content "src/misaki/core.clj"))
    )
  )
