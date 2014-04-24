(ns view.view
  (:require [seesaw.dnd :as dnd])
  (:use [seesaw core mig]))



(defn list-with-moved-element
  ;; this borrows heavily from seesaw/test/examples/reorderable_listbox, though it's a
  ;; standard reordering algorithm
  [current-list element new-index]
  (let [current-list (vec current-list)
        current-index (.indexOf current-list element)]
    (if (= new-index current-index)
      current-list
      (if (< new-index current-index)
        (concat (subvec current-list 0 new-index)
                [element]
                (subvec current-list new-index current-index)
                (subvec current-list (inc current-index)))
        (concat (subvec current-list 0 current-index)
                (subvec current-list (inc current-index) new-index)
                [element]
                (subvec current-list new-index))))))

(defn drop-one [v i]
  " Takes a vector and returns a new vector with the item at index i dropped,
zero-indexed. "
  (vec (concat (subvec v 0 i) (subvec v (inc i) (count v)))))

(defn drop-item [v i]
  " Takes a vector and returns a new vector with the item(s) matching i dropped
"
  (filter #(not (= i %)) v))

;; the following function is (modified) from the seesaw examples
;; (reorderable_listbox.clj)
(defn reorderable-listbox
  " A listbox of items that the user can reorder by dragging and
    dropping.  The caller provide as input an atom containing a sequence
    of immutable data values, e.g. strings.  That sequence will give the
    original order that items appear in the list.  The atom contents will
    be changed to a new sequence whenever the user modifies the order.  No
    new items are allowed to be added, nor may existing items be removed."
  [item-list-atom]
    (listbox :model @item-list-atom
             :drag-enabled? true
             :drop-mode :insert
             :transfer-handler
             (dnd/default-transfer-handler
               ;; This is how listbox reordering is handled - essentially the
               ;; item is being exported, then imported, then the list is 
               ;; updated in the import portion to reflect the new placement
               :import [dnd/string-flavor
                        (fn [{:keys [target data drop? drop-location] :as m}]
                          ;; Ignore anything dropped onto the list
                          ;; that is not in the original set of list
                          ;; items.
                          (if (and drop?
                                   (:insert? drop-location)
                                   (:index drop-location)
                                   ((set @item-list-atom) data))
                            (let [new-order (list-with-moved-element
                                              @item-list-atom data
                                              (:index drop-location))]
                              (reset! item-list-atom new-order)
                              (config! target :model new-order))))]
               :export {:actions (constantly :copy)
                        :start   (fn [c]
                                   [dnd/string-flavor (selection c)])})))


(defn make-todo [list-atom save-lambda]
  "Takes a list, builds it out into a border panel complete with buttons
   and internal event handling and returns it. "
  (let [the-list list-atom
        list-name (:name (meta @the-list))
        the-listbox (reorderable-listbox the-list)
        the-entryfield (text "")
        the-add-button (button :text "add")
        add-fn (fn [e] ;; lambda fn to add new item from the text field (UI)
                 (let [entry-text (text the-entryfield)
                       meta-data (meta @the-list)]
                   (if (> (count entry-text) 0)                ;; if new text
                     (do (swap! the-list #(conj % entry-text)) ;; update atom
                         ;; meta-data has an issue where it disappears
                         (swap! the-list #(with-meta % meta-data)) 
                         ;(save-list! @the-list the-name "active") ;; save atom
                         (save-lambda list-name the-list)
                         (text! the-entryfield "")             ;; clear textbox
                         (config! the-listbox :model @the-list))))) ;;update lbx
        _ (listen the-add-button :action add-fn) ;; bind add button to add-fn
        the-remove-button (button :text "remove")
        remove-fn (fn [e] ;; fn bound to the remove button (UI)
                    (let [selected (set
                                    (selection the-listbox {:multi? true}))
                          meta-data (meta @the-list)]
                      (swap! the-list #(remove selected %))
                      ;; for whatever reason this deletes the meta data, 
                      ;; so it has to be added back manually
                      (swap! the-list #(with-meta % meta-data))
                      ;; (catch-finished selected) ;; sends to the done list
                      (save-lambda list-name the-list)
                      (config! the-listbox :model @the-list)))
        _ (listen the-remove-button :action remove-fn) 
        the-shuffle-button (button :text "shuffle")
        _ (listen the-shuffle-button :action
                  (fn [e]
                    (let [meta-data (meta @the-list)]
                      ;; fix shuffle turning the list into a vector
                      (swap! the-list #(into '() (reverse (shuffle %))))
                      ;; fix the loss of meta-data
                      (swap! the-list #(with-meta % meta-data))
                      (config! the-listbox :model @the-list)
                      (save-lambda list-name the-list))))
        
        ;; functionality's done, now we lay out the interface
        the-north-split (top-bottom-split
                         (label list-name)
                         (left-right-split the-entryfield
                                          the-add-button
                                          :divider-location 2/3))
        the-south-split (left-right-split the-remove-button
                                          the-shuffle-button
                                          :divider-location 2/3)
        the-content (border-panel
                     :north the-north-split
                     :center (scrollable the-listbox)
                     :south the-south-split
                     :vgap 5 :hgap 5 :border 5)]
    {:content the-content
     :name list-name}))



(defn three-split-horiz [one two three]
  " helper function for display-lists, displays 3 lists side by side, 33% each "
  (left-right-split
   (left-right-split one two :divider-location 1/2)
   three :divider-location 2/3))

(defn three-split-vert [one two three]
  " helper function for display-lists, displays 3 lists vertically, 33% each "
  (top-bottom-split
   (top-bottom-split one two :divider-location 1/2)
   three :divider-location 2/3))

(defn display-lists [frames]
  " This is what determines how the todo lists will be tiled. This could maybe
    be more dynamic? "
  (let [the-count (count frames)
        the-content (map :content frames)]
    (cond (= the-count 1) (nth the-content 0)
          (= the-count 2) (left-right-split (nth the-content 0)
                                            (nth the-content 1)
                                            :divider-location 1/2)
          (= the-count 3) (top-bottom-split  ;; two top, one bottom
                           (left-right-split (nth the-content 0)
                                             (nth the-content 1)
                                             :divider-location 1/2)
                           (nth the-content 2) :divider-location 1/2)
          (= the-count 4) (top-bottom-split  ;; two and two
                           (left-right-split (nth the-content 0)
                                             (nth the-content 1)
                                             :divider-location 1/2)
                           (left-right-split (nth the-content 2)
                                             (nth the-content 3)
                                             :divider-location 1/2)
                           :divider-location 1/2)
          (= the-count 5) (top-bottom-split  ;; two top three bottom
                           (left-right-split (nth the-content 0)
                                             (nth the-content 1)
                                             :divider-location 1/2)
                           (three-split-horiz (nth the-content 2)
                                              (nth the-content 3)
                                              (nth the-content 4)))
          (= the-count 6) (top-bottom-split  ;; three top three bottom
                           (three-split-horiz (nth the-content 0)
                                              (nth the-content 1)
                                              (nth the-content 2))
                           (three-split-horiz (nth the-content 3)
                                              (nth the-content 4)
                                              (nth the-content 5))
                           :divider-location 1/2)
          (= the-count 7) (three-split-vert  ;; two-two-three
                           (left-right-split (nth the-content 0)
                                             (nth the-content 1)
                                             :divider-location 1/2)
                           (left-right-split (nth the-content 2)
                                             (nth the-content 3)
                                             :divider-location 1/2)
                           (three-split-horiz (nth the-content 4)
                                              (nth the-content 5)
                                              (nth the-content 6)))
          (= the-count 8) (three-split-vert  ;; two-three-three
                           (left-right-split (nth the-content 0)
                                             (nth the-content 1)
                                             :divider-location 1/2)
                           (three-split-horiz (nth the-content 2)
                                              (nth the-content 3)
                                              (nth the-content 4))
                           (three-split-horiz (nth the-content 5)
                                              (nth the-content 6)
                                              (nth the-content 7)))
          (= the-count 9) (three-split-vert  ;; three-three-three
                           (three-split-horiz (nth the-content 0)
                                              (nth the-content 1)
                                              (nth the-content 2))
                           (three-split-horiz (nth the-content 3)
                                              (nth the-content 4)
                                              (nth the-content 5))
                           (three-split-horiz (nth the-content 6)
                                              (nth the-content 7)
                                              (nth the-content 8))))))

(defn mod-listen [get-active-lambda get-hidden-lambda save-list-lambda
                  delete-list-lambda make-active-lambda make-hidden-lambda
                  update-window-lambda]
  " Create the pop-up window for the 'edit lists' menu option "
  ;; pop-up a new window for meta list options.  Everything for the
  ;; 'modify' pop-up is encapsulated in this closure.  This is a bit 
  ;; complicated and maybe should be broken out into pieces, as it is
  ;; it wouldn't really fit into the usual 80 columns
  (fn [e]
    (let [active-model (atom (get-active-lambda))
          active-listbox (reorderable-listbox active-model)
          hidden-model (atom (get-hidden-lambda))
          hidden-listbox (reorderable-listbox hidden-model)
          make-hidden (button :text "make selected hidden")
          make-active (button :text "make selected active")
          delete-list (button :text "delete selected hidden list")
          add-new-list (button :text "add new active list")
          _ (listen make-hidden :action
                    ;; "make-hidden moves a string from the active
                    ;;  list to the hidden list.  It does not happen
                    ;;  in a transaction and it's a multistep process
                    ;;  so there's a possibility of state problems."
                    (fn [e]
                      (let [mover (selection active-listbox)]
                        (swap! hidden-model #(conj % mover))
                        (swap! active-model #(drop-item % mover))
                        (config! hidden-listbox :model @hidden-model)
                        (config! active-listbox :model @active-model)
                        (make-hidden-lambda mover))))
          _ (listen make-active :action
                    ;; "See notes for make-hidden above.
                    ;;  They are almost identical."
                    (fn [e] (let [mover (selection hidden-listbox)]
                              (swap! active-model #(conj % mover))
                              (swap! hidden-model #(drop-item % mover))
                              (config! hidden-listbox :model @hidden-model)
                              (config! active-listbox :model @active-model)
                              (make-active-lambda mover))))
          delete-dialog (dialog :content
                                (flow-panel :items
                                            ["Delete the selected not-active todo-list? This will delete the list and all of its items and this can't be undone!"])
                                :option-type :ok-cancel
                                :success-fn (fn [e]
                                              (let [deletion (selection hidden-listbox)]
                                                (swap! hidden-model #(drop-item % deletion))
                                                (delete-list-lambda deletion)
                                                (config! hidden-listbox :model @hidden-model))))
          _ (listen delete-list :action (fn [e]
                                          (-> delete-dialog
                                              pack!
                                              show!)))
          entry-field (text "add new name here")
          add-new-dialog (dialog :content
                                 (flow-panel :items
                                             ["add new todo-list with name: " entry-field])
                                 :option-type :ok-cancel
                                 :success-fn (fn [e]
                                               (let [new-name (text entry-field)]
                                                 (swap! active-model #(conj % new-name))
                                                 (config! active-listbox :model @active-model)
                                                 ;; this saves the new empty list to disk
                                                 (save-list-lambda (atom (with-meta '() {:status "active"
                                                                                        :name new-name}))
                                                                   new-name
                                                                   "active")
                                                 true)))
          _ (listen add-new-list :action (fn [e]
                                           (-> add-new-dialog
                                               pack!
                                               show!)))]
      ;; the following is a dialog because it's an easy way to implement
      ;; a function (the window update) on its closing
      ;; if a "done" button could be wired up to update the main window
      ;; and close the pop-up window that might be cleaner, since this
      ;; isn't sizing anything very well
      (-> (dialog :content (top-bottom-split
                            (border-panel :north "Active Todo Lists"
                                          :center active-listbox
                                          :south (left-right-split make-hidden add-new-list))
                            (border-panel :north "Hidden Todo Lists"
                                          :center hidden-listbox
                                          :south (left-right-split make-active delete-list))
                            :divider-location 1/2)
                  :success-fn (fn [e] (update-window-lambda)))
          pack!
          show!))))


(defn make-menubar [get-active-lambda get-hidden-lambda save-list-lambda
                    delete-list-lambda make-active-lambda make-hidden-lambda
                    update-window-lambda]
  
  (let [modify (action :handler (mod-listen get-active-lambda 
                                            get-hidden-lambda
                                            save-list-lambda
                                            delete-list-lambda
                                            make-active-lambda
                                            make-hidden-lambda
                                            update-window-lambda)
                       :name "edit lists"
                       :tip "Change which lists are displayed.")]
      (menubar
       :items [(menu :text "file" :items [modify])])))

;; Interface methods
;;
;; Build window
;; build a list
;; add a list
;; remove a list
;; reorder lists
;; display menu
;; display list options

;; state
;; active lists list
;; hidden lists list
;; each todo-list's items





(def main-frame 
  (frame :title "Tiling Todo Lists"
         :height 650
         :width 650
         :on-close :exit))

(defn show-window! []
  (-> main-frame
      pack!
      show!))

(defn build-listbox [list-atom]
  (reorderable-listbox list-atom))

(defn add-menubar! [get-active-lambda get-hidden-lambda save-list-lambda
                    delete-list-lambda make-active-lambda make-hidden-lambda
                    update-window-lambda]
  ;; these lambda functions are given by the controller when add-menubar! 
  ;; is called, it allows the meta list options dialog to get the current 
  ;; state of the program without :requiring the namespace in the view file 
  ;; which would cause a circular dependency.  update-window-lambda is run
  ;; upon closing of the 'edit lists' dialog.
  (config! main-frame :menubar (make-menubar get-active-lambda get-hidden-lambda
                                             save-list-lambda delete-list-lambda
                                             make-active-lambda 
                                             make-hidden-lambda
                                             update-window-lambda)))

(defn display [lists save-lambda]
  " Lists is a list or vector of todo-list-atoms.  This function displays 
    those todo lists in place of what was already showing. This gets 
    called at start-up and when the list configuration changes. "
  (let [todo-boxes (map (fn [x] (make-todo x save-lambda)) lists)]
    (config! main-frame :content (display-lists todo-boxes))
    (pack! main-frame)
    (show! main-frame)))

(defn add-to-window! [list-atom]
  (println "add to window method\n\n\n")
  (display (build-listbox list-atom)))


(defn _native! []
  ;; this doesn't get passed to core.clj otherwise(?)
  (native!))
