(ns plisp.views.source-panel
  (:require
   [re-frame.core :as rf]
   [reagent-mui.icons.stop-circle :refer [stop-circle]]
   [reagent-mui.material.table :refer [table]]
   [reagent-mui.material.table-body :refer [table-body]]
   [reagent-mui.material.table-cell :refer [table-cell]]
   [reagent-mui.material.table-row :refer [table-row]]
   [plisp.asm.lisp :as lisp]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.util :refer [hex-word str->html]]))

(defn- source-line [i source]
  (let [breakpoints @(rf/subscribe [::subs/breakpoints])
        address (get lisp/lisp-debug-info i)]
    ^{:key (str "source-" i)}
    [table-row {:hover true
                :on-click
                 #(rf/dispatch [::processor-service/toggle-breakpoint address])}
     [table-cell {:style {:border-bottom :none}}
      (hex-word address)]
     [table-cell {:style {:border-bottom :none}}
      (when (breakpoints address) [stop-circle])]
     [table-cell {:style {:border-bottom :none}}
      (str->html "     ")]
     [table-cell {:style {:border-bottom :none}}
      (:source-line source)]]))

(defn source-panel []
  [:div {:style {:flex "1 1 auto"
                 :overflow "auto"
                 :min-height 0}}
   [table {:size :small}
    [table-body
     (doall (map-indexed source-line lisp/lisp-source))]]])
