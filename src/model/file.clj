(ns model.file
  (:require [clojure.java.io :as io]))

;; dynamic for testing purposes
(def ^:dynamic storage-folder "lists/")

(defn loaded-lists []
  " Returns a file-seq of all of the todo-lists in memory - active and hidden
    aren't sorted out at this point. This is only run at start-up and is not
    updated when lists are created/deleted - those changes are stored in the
    'organizer'. "
  (let [return-value (-> storage-folder
                         clojure.java.io/file
                         file-seq
                         rest)]
    return-value))
  
(defn get-name []
  " .getName isn't being found (not available at read-time?) so it's wrapped
    up here. "
  (fn [x] (.getName x)))

(defn get-list-names []
  " Returns a list of todo-list names in memory at the time of program
    launch. "
  (let [return-value 
        (map (get-name) (loaded-lists))]
    return-value))

(defn save-list! [the-list list-name status]
  " Saves list data to local memory. "
  (spit (str storage-folder list-name)
        (prn-str (map str (conj the-list status)))))

(defn delete-list! [list-name]
  ;; DANGEROUS!!!  This could use some checking
  (io/delete-file (str storage-folder list-name)))

(defn get-list [list-name]
  " Returns a list of todo-list items from memory for a given list name. "
  ;; storage-folder is used here instead of (:storage organizer) because
  ;; organizer isn't initializing/returning properly...
  (let [raw-file (try (slurp (str storage-folder list-name))
                      (catch Exception e))]
     (if raw-file
       (binding [*read-eval* false]  ;;  security?
         (-> raw-file
             read-string             ;; convert file to clojure list
             (#(with-meta % {:tag (first %)})) ;; move active/hidden to metadata
             rest                    ;; first element is "active", remove it
             ((fn [y] (map str y)))));; convert list members to str's
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
