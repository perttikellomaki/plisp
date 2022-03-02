(ns plisp.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.form-group :refer [form-group]]
   [reagent-mui.material.form-control-label :refer [form-control-label]]
   [reagent-mui.material.icon-button :refer [icon-button]]
   [reagent-mui.icons.pause-circle :refer [pause-circle]]
   [reagent-mui.icons.play-circle :refer [play-circle]]
   [reagent-mui.icons.restart-alt :refer [restart-alt]]
   [reagent-mui.material.switch-component :refer [switch]]
   [reagent-mui.material.tab :refer [tab]]
   [reagent-mui.material.tabs :refer [tabs]]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.views.lisp-panel :refer [lisp-panel]]
   [plisp.views.processor-panel :refer [processor-panel]]))

(def selected-panel (reagent/atom 0))

(defn- switch-panel [_event value]
  (swap! selected-panel (fn [_ p] p) value))

(defn main-panel []
  (let [running @(rf/subscribe [::subs/execution-running])
                instruction-count @(rf/subscribe [::subs/instruction-count])]
    [:div
     [:div
      [icon-button
       {:on-click #(rf/dispatch  [::processor-service/run])
        :disabled running}
       [play-circle]]
      [icon-button
       {:on-click #(rf/dispatch  [::processor-service/pause])
        :disabled (not running)}
       [pause-circle]]
      [icon-button
       {:on-click #(rf/dispatch  [::processor-service/reset])}
       [restart-alt]]
      [:span instruction-count " instructions executed"]]
     [tabs {:value @selected-panel
            :on-change switch-panel}
      [tab {:label "Lisp"}]
      [tab {:label "Processor"}]]
     (case @selected-panel
       0 [lisp-panel]
       1 [processor-panel])]))
