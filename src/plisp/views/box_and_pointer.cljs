(ns plisp.views.box-and-pointer
  (:require
   [re-frame.core :as rf]
   [reagent-mui.material.menu-item :refer [menu-item]]
   [reagent-mui.material.text-field :refer [text-field]]
   [plisp.cosmac.processor :as processor]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.util :refer [event-value int16 register-path] :as util]))

(def x-scale 40)
(def y-scale 20)

(defn- layout-boxes [{:keys [x y address parent-x parent-y]} memory]
  (cond

    (or (= address 0x0000)
        (= address 0x0004))
    {:width 0
     :pointers []
     :boxes []}

    (>= address 0x8000)
    (let [car-contents (processor/word-in-memory address memory)
          cdr-contents (processor/word-in-memory (+ address 2) memory)
          car-boxes (layout-boxes
                     {:x x
                      :y (+ y 2)
                      :address car-contents
                      :parent-x (+ x 0.5)
                      :parent-y (+ y 0.5)}
                     memory)
          cdr-boxes (layout-boxes
                     {:x (+ x (:width car-boxes))
                      :y y
                      :address cdr-contents
                      :parent-x (+ (inc x) 0.5)
                      :parent-y (+ y 0.5)}
                     memory)]
      {:width (max 3
                   (+ (:width car-boxes)
                      (:width cdr-boxes)))
       :pointers (into (if parent-x
                         [{:from-x parent-x
                           :from-y parent-y
                           :to-x (+ x 0.5)
                           :to-y (+ y 0.5)}]
                         [])
                       (concat (:pointers car-boxes) (:pointers cdr-boxes)))
       :boxes (into [{:x x
                      :y y
                      :address address
                      :car car-contents
                      :cdr cdr-contents}]
                    (concat (:boxes car-boxes) (:boxes cdr-boxes)))})

    :else {:width 0
           :boxes []}))

(defn- svg-cons [x y car-value cdr-value]
  (let [text-x (+ x 3)
        text-y (+ y 16)]
    (list
     ^{:key (str "cons-" x "-" y "-car")}
     [:rect {:x x :y y :width x-scale :height y-scale :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-car-text")}
     [:text {:x text-x :y text-y :fill "green"} car-value]
     ^{:key (str "cons-" x "-" y "-cdr")}
     [:rect {:x (+ x x-scale) :y y :width x-scale :height y-scale :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-cdr-text")}
     [:text {:x (+ text-x x-scale) :y text-y :fill "green"} cdr-value])))

  
(defn box-and-pointer-panel [processor]
  (let [inspection-source @(rf/subscribe [::subs/inspection-source])
        address (-> (get-in processor (register-path inspection-source))
                    int16)
        boxes (layout-boxes {:x 0 :y 0 :address address}
                            (:mem processor))]
    [:div
     [:div
      [text-field
       {:value       inspection-source
        :label       "Select"
        :placeholder "Placeholder"
        :on-change   (fn [e]
                       (rf/dispatch [::processor-service/set-inspection-source (event-value e)]))
        :select      true}
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
       [menu-item {:value "Rf"} "Rf"]]]
     [:svg {:width "500" :height "300"}
      (doall (map (fn [{:keys [from-x from-y to-x to-y]}]
                    ^{:key (str "line-" from-x "-" from-y "-"
                                to-x "-" to-y)}
                    [:line {:x1 (* x-scale from-x)
                            :y1 (* y-scale from-y)
                            :x2 (* x-scale to-x)
                            :y2 (* y-scale to-y)
                            :stroke "blue"
                            :stroke-width 1}])
                  (:pointers boxes)))
      (doall (map (fn [{:keys [x y car cdr] :as _box}]
                    (svg-cons (* x-scale x) (* y-scale y)
                              (util/hex-word car)
                              (util/hex-word cdr)))
                  (:boxes boxes)))]]))


