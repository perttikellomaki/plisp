(ns plisp.views.lisp-panel
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.text-field :refer [text-field]]
   [reagent-mui.material.form-group :refer [form-group]]
   [reagent-mui.material.form-control-label :refer [form-control-label]]
   [reagent-mui.material.switch-component :refer [switch]]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]))

(defonce text-state (reagent/atom ""))

(defn event-value
  [e]
  (.. e -target -value))

(defn lisp-panel []
  (let [lisp-output @(rf/subscribe [::subs/lisp-output])
        running @(rf/subscribe [::subs/execution-running])]
    [:div
     [form-group
      [form-control-label
       {:control (reagent/as-element [switch {:checked running
                                              :on-change #(rf/dispatch [::processor-service/toggle-running])}])
        :label "Running"}]]
     [button {:on-click #(rf/dispatch [::processor-service/reset])} "Reset"]
     [:pre lisp-output]
     [text-field
      {:value       @text-state
       :multiline   true
       :full-width  true
       :placeholder "Input to Lisp"
       :on-change   (fn [e]
                      (reset! text-state (event-value e)))}]
     [button {:on-click #(rf/dispatch [::processor-service/send-input-to-lisp @text-state])} "Send to Lisp"]]))
