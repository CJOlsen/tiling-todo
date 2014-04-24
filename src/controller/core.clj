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


(def update-window-lambda
  " This updates the main window with the current status of the program.  It's given to the menubar so when the 'edit lists' dialog is opened this can be run upon its exit. "
  (fn [] (view/display
          (model/get-active-lists)
          save-list-lambda)))

(defn -main  [& args]
  (view/_native!)
  (view/show-window!)
  (view/add-menubar! model/get-active-list-names
                     model/get-hidden-list-names
                     model/save-list!
                     update-window-lambda)
  (view/display @active-lists save-list-lambda))
