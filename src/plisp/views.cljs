(ns plisp.views
  (:require
   [reagent.core :as reagent]
   [reagent-mui.material.tab :refer [tab]]
   [reagent-mui.material.tabs :refer [tabs]]
   [plisp.views.lisp-panel :refer [lisp-panel]]
   [plisp.views.processor-panel :refer [processor-panel]]))

(def selected-panel (reagent/atom 0))

(defn- switch-panel [_event value]
  (swap! selected-panel (fn [_ p] p) value))

(defn main-panel []
  [:div
   [tabs {:value @selected-panel
          :on-change switch-panel}
    [tab {:label "Lisp"}]
    [tab {:label "Processor"}]]
   (case @selected-panel
     0 [lisp-panel]
     1 [processor-panel])])
