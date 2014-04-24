(ns controller.core
  (:gen-class)
  (:require [model.file :as model]
            [view.view :as view]))

(def active-lists (atom (model/get-active-lists)))

(def save-list-lambda
  " Given to the view layer for saving list modifications. "
  (fn [list-name the-list]
    (model/save-list! the-list list-name (:status (meta @the-list)))))  ;; will this work???

(def make-active-lambda
  (fn [list-name]
    (-> (model/get-list list-name)
        model/make-list-active!
        (#(model/save-list! % list-name "active")))))

(def make-hidden-lambda
  (fn [list-name]
    (-> (model/get-list list-name)
        model/make-list-hidden!
        (#(model/save-list! % list-name "hidden")))))

(def update-window-lambda
  " This updates the main window with the current status of the program.  It's given to the menubar so when the 'edit lists' dialog is opened this can be run upon its exit. "
  (fn [] (view/display
          (model/get-active-lists)
          save-list-lambda)))

(defn -main  [& args]
  (view/_native!)
  (view/show-window!)
  (view/add-menubar!
   ;; this map is referred to as "core-callbacks" in the view layer
   {:get-active-names model/get-active-list-names
    :get-hidden-names model/get-hidden-list-names
    :save-list! model/save-list!
    :delete-list! model/delete-list!
    :make-active! make-active-lambda
    :make-hidden! make-hidden-lambda
    :update-window! update-window-lambda})
  (view/display @active-lists save-list-lambda))
