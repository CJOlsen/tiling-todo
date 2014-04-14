(ns model.file
  (:require [clojure.java.io :as io]))

;; dynamic for testing purposes
(def ^:dynamic storage-folder "lists/")
(def ^:dynamic storage-folder "test/static-folder")


(defn load-lists []
  " Returns a file-seq of all of the todo-list names in memory - active and 
    hidden aren't sorted out at this point.  *They are java.io.File objects*"
  (-> storage-folder
      clojure.java.io/file
      file-seq
      rest))
  
(defn get-name []
  " .getName isn't being found (not available at read-time?) so it's wrapped
    up here. "
  (fn [x] (.getName x)))

(defn get-list-names []
  " Returns a list of todo-list names in memory at the time of program
    launch. "
  (map (get-name) (load-lists)))

(defn save-list! [the-list list-name status]
  " Saves list data to local memory. "
  (spit (str storage-folder "/" list-name)
        (prn-str (map str (conj @the-list status)))))

(defn delete-list! [list-name]
  ;; DANGEROUS!!!  This could use some checking
  (io/delete-file (str storage-folder list-name)))

(defn get-list [list-name]
  " Returns a list of todo-list items from memory for a given list name. "
  ;; this could be cleaned up a bit
  (let [raw-file (try (slurp (str storage-folder "/" list-name))
                      (catch Exception e))]
     (if raw-file
       (binding [*read-eval* false]  ;;  security?  affects read-string
         (let [as-clj-list (read-string raw-file) ;; convert file to clojure list
               status (first as-clj-list)]        ;; save status for meta-data
           (-> as-clj-list           
               rest                         ;; first element is status, remove it
               (#(with-meta % {:tag status  ;; active or hidden
                               :name list-name}))
               atom))) ;; and make it mutable
         "this is an error" ;; else return an empty list (or error)
         )))


(defn create-list [items status]
  (atom (with-meta items {:tag status})))

(defn make-list-active! [a-list]
  (swap! a-list #(with-meta % {:tag "active"})))

(defn make-list-hidden! [a-list]
  (swap! a-list #(with-meta % {:tag "hidden"})))

(defn list-active? [a-list]
  (= "active" (:tag (meta @a-list))))

(defn get-active-lists []
  (filter list-active? (map get-list (get-list-names))))

(defn startup-lists []
  '())
