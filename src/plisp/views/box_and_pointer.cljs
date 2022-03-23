(ns plisp.views.box-and-pointer
  (:require
   [re-frame.core :as rf]
   [plisp.cosmac.processor :as processor]
   [plisp.subs :as subs]
   [plisp.util :as util]
   [plisp.views.inspection-source-selector :refer [inspection-source-selector]]))

(def x-scale 40)
(def y-scale 20)

(defn- layout-boxes [{:keys [x y address parent-x parent-y]} existing-boxes memory]
  (js/console.log "##" (str (util/hex-word address)) (str (map #(util/hex-word (:address %)) existing-boxes)) (str existing-boxes))
  (cond

    (or (= address 0x0000)
        (= address 0x0004)
        #_(some #(= (:address %) address) existing-boxes))
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
  (let [id ::box-and-pointer-inspector
        inspection-sources @(rf/subscribe [::subs/inspection-sources])
        address (-> inspection-sources
                    (get id)
                    :address)
        boxes (layout-boxes {:x 0 :y 0 :address address}
                            []
                            (:mem processor))]
    [:div
     [inspection-source-selector processor id]
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


