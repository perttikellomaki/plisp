(ns plisp.subs
  (:require
   [re-frame.core :as re-frame]
   [plisp.util :as util]))

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
           PC        (util/int16 (get-in processor [:R P]))]
       (get-in processor [:mem PC])))))

(re-frame/reg-sub
 ::lisp-input
 (fn [db]
   (get-in db [:processor :input-buffer])))

(re-frame/reg-sub
 ::processor
 (fn [db]
   (get-in db [:processor])))

(re-frame/reg-sub
 ::processor-status
 (fn [db]
   (get-in db [:processor :status])))

(re-frame/reg-sub
 ::execution-running
 (fn [db]
   (get-in db [:execution :running])))

(re-frame/reg-sub
 ::breakpoints
 (fn [db]
   (get-in db [:execution :breakpoints])))

(re-frame/reg-sub
 ::inspection-source
 (fn [db]
   (get-in db [:processor-panel :inspection-source])))
