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
  (fn [list-name list-contents]
    '()))  ;; will this work???

;; (model/get-active-lists)
;; (model/display lists)


;; view/show-window!
;; view/show-lists



(defn -main  [& args]
  (view/_native!)
  (view/show-window!)
  (view/display @active-lists save-list-lambda)
)
