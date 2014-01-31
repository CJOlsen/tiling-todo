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
            (save-list! [1 2 3] "delete-test" "hidden")
            (if (member? "delete-test" (get-list-names))
              (do (delete-list! "delete-test")
                  (not (member? "delete-test" (get-list-names))))
              false))))
  
  (testing "Save list to memory and then load it."
    (is (= (doall (frequencies '("one" "shoe" "nightengale" "foo" "bar")))
           (doall (with-redefs [storage-folder "test/dynamic-folder/"]
                    (save-list! '("gibberish") "save-test" "active")
                    (delete-list! "save-test")
                    (save-list! '("one" "nightengale" "shoe" "foo" "bar")
                                "save-test"
                                "active")
                    (frequencies (get-list "save-test"))))))))

(deftest list-operations
  (testing "Add, change and retrieve active/hidden metadata"
    (let [new-list (create-list '(1 2 3 4 5) "active")]
      (is (= true
             (list-active? new-list)))
      (is (= false
             (do (make-list-hidden! new-list)
                 (list-active? new-list)))))))

