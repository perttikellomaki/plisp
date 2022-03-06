(ns plisp.views.lisp-panel
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.text-field :refer [text-field]]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.util :refer [event-value]]))

(defonce text-state (reagent/atom ""))

(defn lisp-panel []
  (let [lisp-output @(rf/subscribe [::subs/lisp-output])]
    [:div {:style {:flex "1"
                   :overflow "auto"
                   :min-height 0}}
     [:pre lisp-output]
     [text-field
      {:value       @text-state
       :multiline   true
       :full-width  true
       :placeholder "Input to Lisp"
       :on-change   (fn [e]
                      (reset! text-state (event-value e)))}]
     [button {:on-click #(rf/dispatch [::processor-service/send-input-to-lisp @text-state])} "Send to Lisp"]]))
