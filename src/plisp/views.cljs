(ns plisp.views
  (:require
   [re-frame.core :as re-frame]
   [plisp.subs :as subs]
   [plisp.asm.lisp :as lisp]
   ))

(def lisp-output
  (lisp/run-lisp "(CONS (QUOTE HELLO) (CONS (QUOTE WORLD) NIL))"))  

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:pre
      lisp-output]
     ]))
