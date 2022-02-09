(ns plisp.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::lisp-output
 (fn [db]
   (:lisp-output db)))
