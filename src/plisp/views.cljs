(ns plisp.views
  (:require
   [clojure.string :as string]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.text-field :refer [text-field]]
   [reagent-mui.material.form-group :refer [form-group]]
   [reagent-mui.material.form-control-label :refer [form-control-label]]
   [reagent-mui.material.switch-component :refer [switch]]
   [plisp.asm.lisp :as lisp]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   ))

(defonce text-state (reagent/atom ""))

(defn event-value
  [e]
  (.. e -target -value))

(defn main-panel []
  (let [lisp-output @(rf/subscribe [::subs/lisp-output])
        lisp-input @(rf/subscribe [::subs/lisp-input])
        instruction-count @(rf/subscribe [::subs/instruction-count])
        current-instruction @(rf/subscribe [::subs/current-instruction])]
    [:div
     [form-group
      [form-control-label
       {:control (reagent/as-element [switch {:default-checked false
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
     [button {:on-click #(rf/dispatch [::processor-service/send-input-to-lisp @text-state])} "Send to Lisp"]
     [:div instruction-count " instructions executed"]
     [:div (str current-instruction)]
     [:hr]
     [:pre
      (when current-instruction
        (->> lisp/lisp-source-text
             (drop (:source-line-number current-instruction))
             (take 5)
             (map-indexed (fn [i line]
                            (str (if (zero? i)
                                   "==>"
                                   "   ")
                                 line)))
             (string/join "\n")))]
     [:hr]
     [:pre (str lisp-input)]]))
