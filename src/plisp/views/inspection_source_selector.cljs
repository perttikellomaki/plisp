(ns plisp.views.inspection-source-selector
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [plisp.services.processor-service :as processor-service]
   [reagent-mui.material.menu-item :refer [menu-item]]
   [reagent-mui.material.text-field :refer [text-field]]
   [plisp.subs :as subs]
   [plisp.util :refer [event-value int16 register-path] :as util]))

(def address (r/atom {}))

(defn- string->address [s]
  (let [addr (js/parseInt s 16)]
    (if (js/isNaN addr)
      0x0000
      addr)))

(defn inspection-source-selector [processor id]
  (let [inspection-sources @(rf/subscribe [::subs/inspection-sources])
        source (or (get inspection-sources id) {:source "R0"})]
    [:div
     [text-field
      {:value       (:source source)
       :label       "Select"
       :placeholder "Placeholder"
       :on-change   (fn [e]
                      (let [source (event-value e)
                            addr (if (= source "address")
                                   (string->address (get @address id))
                                   (-> (get-in processor
                                               (or (register-path source)
                                                   {:hi 0 :lo 0}))
                                       int16))]
                        (rf/dispatch [::processor-service/set-inspection-source
                                      id
                                      (event-value e)
                                      addr])))
       :select      true}
      [menu-item {:value "address"} "address"]
      [menu-item {:value "R0"} "R0"]
      [menu-item {:value "R1"} "R1"]
      [menu-item {:value "R2"} "R2"]
      [menu-item {:value "R3"} "R3"]
      [menu-item {:value "R4"} "R4"]
      [menu-item {:value "R5"} "R5"]
      [menu-item {:value "R6"} "R6"]
      [menu-item {:value "R7"} "R7"]
      [menu-item {:value "R8"} "R8"]
      [menu-item {:value "R9"} "R9"]
      [menu-item {:value "Ra"} "Ra"]
      [menu-item {:value "Rb"} "Rb"]
      [menu-item {:value "Rc"} "Rc"]
      [menu-item {:value "Rd"} "Rd"]
      [menu-item {:value "Re"} "Re"]
      [menu-item {:value "Rf"} "Rf"]]
     [text-field
      {:style {:width 80}
       :disabled (not= (:source source) "address")
       :value (or (get @address id) "")
       :on-change (fn [e]
                    (swap! address #(assoc % id (event-value e)))
                    (rf/dispatch [::processor-service/set-inspection-source
                                  id
                                  (:source source)
                                  (string->address (get @address id))]))}]]))
