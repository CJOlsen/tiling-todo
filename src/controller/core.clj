(ns controller.core
  (:gen-class)
  (:require [model.file :as model]
            [view.view :as view])
)

(defn get-active-lists-c []
  " Get active lists from the model layer, perform any controller level 
    checks or cleanup here. "
  (model/get-active-lists))




(defn -main  [& args]
  ;; (native!)
  ;; (-> view/the-frame
  ;;     pack!
  ;;     show!)
)
