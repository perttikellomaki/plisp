(ns plisp.views.source-panel
  (:require
   [re-frame.core :as rf]
   [plisp.asm.lisp :as lisp]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.util :refer [hex-word str->html]]))

(defn- source-line [i source]
  (let [breakpoints @(rf/subscribe [::subs/breakpoints])
        address (get lisp/lisp-debug-info i)]
    ^{:key (str "source-" i)}
    [:tr
     [:td [:a {:on-click
               #(rf/dispatch [::processor-service/set-breakpoint address])}
           (hex-word address)]]
     [:td (when (breakpoints address) "*")]
     [:td (str->html "     ")]
     [:td (:source-line source)]]))

(defn source-panel []
  [:table
   [:tbody
    (doall (map-indexed source-line lisp/lisp-source))]])
