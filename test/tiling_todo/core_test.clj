(ns tiling-todo.core-test
  (:require [clojure.test :refer :all])
  (:use [model.file]))

(defn member? [item _list]
  (not (nil? (some #{item} _list))))

(deftest file-operations
  (testing "Get file names from memory"
    (with-redefs [storage-folder "test/static-folder/"]
      (is (= (frequencies '("one" "two" "three" "four"))
             (frequencies (get-list-names))))))

  (testing "Delete list from memory (must save it if not there already)"
    (is (with-redefs [storage-folder "test/dynamic-folder/"]
          (save-list! (atom [1 2 3]) "delete-test" "hidden")
          (if (member? "delete-test" (get-list-names))
            (do (delete-list! "delete-test")
                (not (member? "delete-test" (get-list-names)))) ;;must be true
            false))))
  
  (testing "Save list to memory and then load it."
    (is (= (doall (frequencies '("one" "shoe" "nightengale" "foo" "bar")))
           (doall (with-redefs [storage-folder "test/dynamic-folder/"]
                    (save-list! (atom '("gibberish")) "save-test" "active")
                    (delete-list! "save-test")
                    (save-list! (atom '("one" "nightengale" "shoe" "foo" "bar"))
                                "save-test"
                                "active")
                    (frequencies @(get-list "save-test"))))))))

(deftest list-operations
  (testing "Add, change and retrieve active/hidden metadata"
    (let [new-list (create-list '(1 2 3 4 5) "active")]
      (is (= true
             (list-active? new-list)))
      (is (= false
             (do (make-list-hidden! new-list)
                 (list-active? new-list))))))
  (testing "Retrieve all active lists from memory"
    (with-redefs [storage-folder "test/static-folder"]
      (is (= (frequencies '("four" "two"))
             (frequencies (-> (map deref (get-active-lists))
                              (#(map meta %))
                              (#(map :name %))))))))
  (testing "Add item to a list"
    (let [new-list (create-list '(1 2 3) "active")]
      (add-to-list! new-list "hello")
      (is (= (frequencies '(1 2 3 "hello"))
             (frequencies @new-list)))))
  (testing "Remove item from list"
     (let [newer-list (create-list '(1 2 3 4 5 "hello") "hidden")]
      (remove-from-list! newer-list "hello")
      (is (= (frequencies '(1 2 3 4 5))
             (frequencies @newer-list)))))
  (testing "Change list order"
    ;; this could hit the edge cases a little harder
    (let [new-list (create-list '(1 2 3 4 5 "hello") "hidden")]
      (reorder-list! new-list 5 3)
      (is (= @new-list
             '(1 2 3 "hello" 4 5)))
      (reorder-list! new-list 2 4)
      (is (= @new-list
             '(1 2 "hello" 4 3 5)))
      (is (reorder-list! new-list 42 3)
          "list reorder error: index beyond list bounds")
      (is (reorder-list! new-list 3 42)
          "list reorder error: new index beyond list bounds"))))
