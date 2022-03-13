(ns plisp.views.box-and-pointer
  (:require
   [plisp.cosmac.processor :as processor]
   [plisp.util :as util]))

(defn- layout-boxes [{:keys [x y address]} memory]
  (cond

    (or (= address 0x0000)
        (= address 0x0004))
    {:width 0
     :boxes []}

    (>= address 0x8000)
    (let [car-contents (processor/word-in-memory address memory)
          cdr-contents (processor/word-in-memory (+ address 2) memory)
          car-boxes (layout-boxes
                     {:x x
                      :y (inc y)
                      :address car-contents}
                     memory)
          cdr-boxes (layout-boxes
                     {:x (+ x (:width car-boxes))
                      :y y
                      :address cdr-contents}
                     memory)]
      {:width (max 1
                   (+ (:width car-boxes)
                      (:width cdr-boxes)))
       :boxes (into [{:x x
                      :y y
                      :address address
                      :car car-contents
                      :cdr cdr-contents}]
                    (concat (:boxes car-boxes) (:boxes cdr-boxes)))})

    :else {:width 0
           :boxes []}))

(defn- svg-cons [x y car-value cdr-value]
  (let [width 42
        height 23
        text-x (+ x 3)
        text-y (+ y 18)]
    (list
     ^{:key (str "cons-" x "-" y "-car")}
     [:rect {:x x :y y :width width :height height :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-car-text")}
     [:text {:x text-x :y text-y :fill "green"} car-value]
     ^{:key (str "cons-" x "-" y "-cdr")}
     [:rect {:x (+ x width) :y y :width width :height height :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-cdr-text")}
     [:text {:x (+ text-x width) :y text-y :fill "green"} cdr-value])))

  
(defn box-and-pointer-panel [processor]
  (let [boxes (layout-boxes {:x 0 :y 0 :address 0x8354}
                            (:mem processor))]
    [:svg {:width "300" :height "300"}
     (doall (map (fn [{:keys [x y car cdr] :as _box}]
                   (svg-cons (* 110 x) (* 50 y)
                             (util/hex-word car)
                             (util/hex-word cdr)))
                 (:boxes boxes)))]))


