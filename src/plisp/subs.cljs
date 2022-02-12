(ns plisp.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::lisp-output
 (fn [db]
   (:lisp-output db)))

(re-frame/reg-sub
 ::instruction-count
 (fn [db]
   (get-in db [:processor :instruction-count])))

(re-frame/reg-sub
 ::current-instruction
 (fn [db]
   (when-let [processor (get-in db [:processor])]
     (let [P         (:P processor)
           PC        (get-in processor [:R P])]
       (get-in processor [:mem PC])))))

(re-frame/reg-sub
 ::lisp-input
 (fn [db]
   (get-in db [:processor :input-buffer])))
