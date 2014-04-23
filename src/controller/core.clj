(ns controller.core
  (:gen-class)
  (:require [model.file :as model]
            [view.view :as view])
)

(def active-names (atom (model/get-active-list-names)))
(def hidden-names (atom (model/get-hidden-list-names)))
(def active-lists (atom (model/get-active-lists)))

(def save-list-lambda
  " Given to the view layer for saving list modifications. "
  (fn [list-name the-list]
    (println "\n\nsave-list-lambda\n\nlist-name: " list-name "\n\nthe-list: " the-list "\n\nlist-status:\n\n" (:status (meta @the-list)) "\n\n\nand just meta:\n" (meta @the-list))
    (model/save-list! the-list list-name (:status (meta @the-list)))))  ;; will this work???


(defn get-active-lists-c []
  model/get-active-lists)
;; (model/get-active-lists)
;; (model/display lists)


;; view/show-window!
;; view/show-lists



(defn -main  [& args]
  (view/_native!)
  (view/show-window!)
  (view/display @active-lists save-list-lambda)
)
