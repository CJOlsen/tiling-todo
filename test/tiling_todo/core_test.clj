(ns tiling-todo.core-test
  (:require [clojure.test :refer :all])
  (:use [model.file :as m]
        [controller.core :as c]
        [view.view :as view]))

(defn member? [item _list]
  (not (nil? (some #{item} _list))))

;;;; test the model -----------------------------------------------------------
(deftest file-operations
  (testing "Get file names from memory"
    (with-redefs [storage-folder "test/static-folder/"]
      ;; using frequencies gets rid of order dependency
      (is (= (frequencies '("one" "two" "three" "four"))
             (frequencies (m/get-list-names))))))

  (testing "Delete list from memory (must save it if not there already)"
    (is (with-redefs [storage-folder "test/dynamic-folder/"]
          (m/save-list! (atom [1 2 3]) "delete-test" "hidden")
          (if (member? "delete-test" (m/get-list-names))
            (do (m/delete-list! "delete-test")
                (not (member? "delete-test" (m/get-list-names)))) ;;must be true
            false))))
  
  (testing "Save list to memory and then load it."
    (is (= (doall (frequencies '("one" "shoe" "nightengale" "foo" "bar")))
           (doall (with-redefs [storage-folder "test/dynamic-folder/"]
                    (m/save-list! (atom '("gibberish")) "save-test" "active")
                    (m/delete-list! "save-test")
                    (m/save-list! (atom '("one" "nightengale" "shoe" "foo" "bar"))
                                "save-test"
                                "active")
                    (frequencies @(m/get-list "save-test"))))))))

(deftest list-operations
  (testing "Add, change and retrieve active/hidden metadata"
    (let [new-list (m/create-list '(1 2 3 4 5) "active")]
      (is (= true
             (m/list-active? new-list)))
      (is (= false
             (do (m/make-list-hidden! new-list)
                 (m/list-active? new-list))))))
  (testing "Retrieve all active lists from memory"
    (with-redefs [storage-folder "test/static-folder"]
      
      (is (= (frequencies '("four" "two"))
             (frequencies (-> (map deref (m/get-active-lists))
                              (#(map meta %))
                              (#(map :name %))))))))
  (testing "Add item to a list"
    (let [new-list (create-list '(1 2 3) "active")]
      (m/add-to-list! new-list "hello")
      (is (= (frequencies '(1 2 3 "hello"))
             (frequencies @new-list)))))
  (testing "Remove item from list"
     (let [newer-list (m/create-list '(1 2 3 4 5 "hello") "hidden")]
      (m/remove-from-list! newer-list "hello")
      (is (= (frequencies '(1 2 3 4 5))
             (frequencies @newer-list)))))
  (testing "Change list order"
    ;; this could hit the edge cases a little harder
    (let [new-list (m/create-list '(1 2 3 4 5 "hello") "hidden")]
      ;; move 5th item to 3rd position
      (m/reorder-list! new-list 5 3)
      (is (= @new-list
             '(1 2 3 "hello" 4 5)))
      ;; move 2nd item to 4th position
      (m/reorder-list! new-list 2 4)
      (is (= @new-list
             '(1 2 "hello" 4 3 5)))
      (is (m/reorder-list! new-list 42 3)
          "list reorder error: index beyond list bounds")
      (is (m/reorder-list! new-list 3 42)
          "list reorder error: new index beyond list bounds"))))

;;;; test the controller -------------------------------------------------------


  ;; **** There's a sizable problem as far as testing is concerned that too 
  ;;      much of the controller logic is in the view layer.  To fix the view
  ;;      and controller need to be refactored to migrate the functionality 
  ;;      into the controller.

  ;; (deftest start-up
  ;; (testing "Load active listboxes into controller"
  ;;   (with-redefs [storage-folder "test/static-folder"]
  ;;     ;; (almost) identical to the model layer test
  ;;     (is (= (frequencies '("four" "two"))
  ;;            (frequencies (-> (map deref (c/get-active-lists-c))
  ;;                             (#(map meta %))
  ;;                             (#(map :name %))))))))
  ;; the following only check for errors - the GUI layer must
  ;; be checked manually to ensure it's displaying properly
  ;; (testing "Open the main window without any lists, without error"
  ;;   (view/show-window!))
  ;; (testing "Build a list without error"
  ;;   (build-listbox (create-list '(1 2 3) "active")))
  ;; (testing "Add a list to the main window without error"
  ;;   (do (show-window!)
  ;;       (add-to-window! (create-list '(1 2 3) "active"))))
  ;; (testing "Add multiple lists to the main window without error"
  ;;   (is (= 5 0)))
  ;; (testing "Remove an active listbox from current display, without error"
  ;;   (is (= 5 0)))
  ;; (testing "Change order of displayed listboxes, withour error"
  ;;   (is (= 5 0)))
  ;; (testing "Display the list-options pop-up"
  ;;   (is (= 5 0))))

