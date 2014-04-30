;; Author: Christopher Olsen
;; Program: Tiling Todo Lists
;; Copyright 2014
;; License: GNU GPLv3 (Eclipse available upon request)

;; This file is part of Tiling Todo Lists.
;;
;; Tiling Todo Lists is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; Tiling Todo Lists is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with Tiling Todo Lists.  If not, see <http://www.gnu.org/licenses/>.


(ns model.file
  (:require [clojure.java.io :as io]))

;; dynamic for testing purposes (these get overridden in the test suite)
(def ^:dynamic storage-folder "lists/")
;(def ^:dynamic storage-folder "test/static-folder")


(defn load-lists []
  " Returns a file-seq of all of the todo-list names in memory - active and 
    hidden aren't sorted out at this point.  *They are java.io.File objects*"
  (-> storage-folder
      clojure.java.io/file
      file-seq
      rest))
  
(defn get-name []
  " .getName isn't being found (not available at read-time?) so it's wrapped
    up here. This is getting the file name."
  (fn [x] (.getName x)))

(defn get-list-names []
  " Returns a list of todo-list names in memory at the time of program
    launch. "
  (map (get-name) (load-lists)))

(defn save-list! [the-list list-name status]
  " Saves list data to local memory. Takes an atom of a list and two strings."
  (spit (str storage-folder "/" list-name)
        (prn-str (map str (conj @the-list status)))))

(defn delete-list! [list-name]
  ;; DANGEROUS!!!  This could use some checking
  (io/delete-file (str storage-folder list-name)))

(defn get-list [list-name]
  " Returns a list (an atom with meta-data) of todo-list items from memory for a given list name. "
  ;; this could be cleaned up a bit
  (let [raw-file (try (slurp (str storage-folder "/" list-name))
                      (catch Exception e))]
     (if raw-file
       (binding [*read-eval* false]  ;;  security?  affects read-string
         (let [as-clj-list (read-string raw-file) ;; convert file to clojure list
               status (first as-clj-list)]        ;; save status for meta-data
           (-> as-clj-list           
               rest                         ;; first element is status, remove it
               ;; there's an argument for placing the meta-data on the atom
               ;; instead of on the list within the atom
               (#(with-meta % {:status status  ;; active or hidden
                               :name list-name}))
               atom))) ;; and make it mutable
         "this is an error" ;; else return an empty list (or error)
         )))


;; above is getting lists from files, below is dealing with them in memory


(defn create-list [items status]
  (atom (with-meta items {:status status})))

(defn update-meta! [item meta-map]  ;; maybe DRY up make-list-active! and make-list-hidden!
  (let [current-meta (meta @item)]
    (swap! item #(with-meta % (assoc current-meta)))))


(defn make-list-active! [a-list]
  (let [current-meta (meta @a-list)]
    (swap! a-list #(with-meta % (assoc current-meta :status "active")))
    a-list))

(defn make-list-hidden! [a-list]
  (let [current-meta (meta @a-list)]
    (swap! a-list #(with-meta % (assoc current-meta :status "hidden")))
    a-list))

(defn list-active? [a-list]
  (= "active" (:status (meta @a-list))))

(defn list-hidden? [a-list]
  (= "hidden" (:status (meta @a-list))))

(defn get-active-lists []
  (filter list-active? (map get-list (get-list-names))))

;; (defn get-active-lists []
;;   (let [return-lists (filter list-active? (map get-list (get-list-names)))
;;         default-list (list (atom (with-meta (list "default list")
;;                              {:status "active" :name "default"}
;;                              )))]
;;     ;; if no lists exist, return a default exist so nil isn't being displayed
;;     (if (empty? return-lists)
;;       default-list
;;       return-lists)))

(defn get-hidden-lists []
  (filter (fn [x] (not (list-active? x)))
          (map get-list (get-list-names))))

(defn get-active-list-names []
  (map (fn [x] (-> x deref meta :name)) (get-active-lists)))

(defn get-hidden-list-names []
  (map (fn [x] (-> x deref meta :name)) (get-hidden-lists)))

(defn startup-lists []
  '())

(defn add-to-list! [a-list item] 
  (swap! a-list #(conj % item)))

(defn remove-from-list! [a-list item]
  (swap! a-list #(filter (fn [x] (not (= x item))) %)))

(defn reorder-list! [a-list index new-index]
  ;; this should have some error checking before it starts reset!-ing 
  ;; the atoms....
  (cond (>= index (count @a-list))
        "list reorder error: index beyond list bounds"
        (>= new-index (count @a-list))
        "list reorder error: new index beyond list bounds"
        (= index new-index)
        a-list
        (> index new-index)
        (reset! a-list
               (concat (take new-index @a-list)
                       ;; '("|") ;; adding these simplifies debugging
                       (list (nth @a-list index))
                       ;; '("|")
                       (-> @a-list
                           (#(drop new-index %))
                           (#(drop-last (- (count @a-list) index) %)))
                       ;; '("|")
                       (drop (inc index) @a-list)))
        (< index new-index)
        (reset! a-list
                (concat (take index @a-list)
                        (-> @a-list
                            (#(drop (inc index) %))
                            (#(drop-last (- (count @a-list) new-index 1) %)))
                        (list (nth @a-list index))
                        (drop (inc new-index) @a-list)))))


(defn prep-save-folder []
  " Checks the list folder and if empty creates a default list. "
  (let [file-count (count (file-seq (io/file "lists/")))]
    (if (= file-count 1)
      (spit "lists/default" "(\"active\" \"default\")"))))

(prep-save-folder)
