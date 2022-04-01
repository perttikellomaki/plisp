(ns plisp.views.box-and-pointer
  (:require
   [re-frame.core :as rf]
   [plisp.cosmac.processor :as processor]
   [plisp.subs :as subs]
   [plisp.util :as util]
   [plisp.views.inspection-source-selector :refer [inspection-source-selector]]))

(def box-width 40)
(def box-height 20)

(defn- x-scale [x] (* x box-width))
(defn- y-scale [y] (* y box-height))

(defn- pointer-x-offset [x]
  (+ x (/ box-width 2)))

(defn- pointer-y-offset [y]
  (+ y (/ box-height 2)))

(defn- layout-graph [{:keys [x y address parent-car parent-cdr parent]} existing-boxes memory]
  (cond

    (or (= address 0x0000)
        (= address 0x0004)
        (some #(= (:address %) address) existing-boxes))
    {:width 0
     :pointers []
     :boxes []}

    (>= address 0x8000)
    (let [car-contents (processor/word-in-memory address memory)
          cdr-contents (processor/word-in-memory (+ address 2) memory)
          car-graph (layout-graph
                     {:x x
                      :y (+ y 2)
                      :address car-contents
                      :parent-car address}
                     existing-boxes
                     memory)
          cdr-graph (layout-graph
                     {:x (+ x (max (:width car-graph) 3))
                      :y y
                      :address cdr-contents
                      :parent-cdr address}
                     (concat (:boxes car-graph)  existing-boxes)
                     memory)]
      {:width (+ (max 3 (:width car-graph))
                 (:width cdr-graph))
       :pointers (into (if (or parent-car parent-cdr)
                         [{:from-car parent-car
                           :from-cdr parent-cdr
                           :to address}]
                         [])
                       (concat (:pointers car-graph) (:pointers cdr-graph)))
       :boxes (into [{:x x
                      :y y
                      :address address
                      :address-hex (util/hex-word address)
                      :car car-contents
                      :cdr cdr-contents}]
                    (concat (:boxes car-graph) (:boxes cdr-graph)))})

    :else {:width 0
           :boxes []}))

(def ml-addresses
  {"67fa" "<cons>"
   "664f" "<quote>"
   "6690" "<car>"
   "66a6" "<cdr>"
   "69f3" "<lambda>"
   "6746" "<setq>"
   "6769" "<defq>"})

(defn- word-from-memory [memory address]
  (+ (* (-> (get memory address) :value) 0x100)
     (-> (get memory (inc address)) :value)))

(defn- byte-from-memory [memory address]
  (-> (get memory address) :value))

(defn- string-buffer-end [memory]
  (word-from-memory memory 0x7100))

(defn- atom-from-string-buffer [memory starting-address]
  (let [name-length (dec (byte-from-memory memory (+ starting-address 2)))]
    {:address (word-from-memory memory starting-address)
     :name (->> (range (+ 3 starting-address) (+ 3 starting-address name-length))
                (map #(byte-from-memory memory %))
                (map #(char %))
                (apply str))
     :next (+ starting-address name-length 3)}))

(defn- atoms [memory starting-address]
  (if (>= starting-address (string-buffer-end memory))
    []
    (let [{:keys [address name next]}
          (atom-from-string-buffer memory starting-address)]
      (conj (atoms memory next)
            {:address address
             :name name}))))

(defn- svg-cons [x y car-value cdr-value address memory]
  (let [atoms (atoms memory 0x7102)
        atom-names  (zipmap (map #(util/hex-word (:address %)) atoms)
                            (map :name atoms))
        text-x (+ x 3)
        text-y (+ y 16)]
    (list
     ^{:key (str "cons-" x "-" y "-car")}
     [:rect {:x x :y y :width box-width :height box-height :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-car-text")}
     [:text {:x text-x :y text-y :fill "green"} car-value]
     ^{:key (str "cons-" x "-" y "-cdr")}
     [:rect {:x (+ x box-width) :y y :width box-width :height box-height :stroke "green" :stroke-width "2" :fill "yellow"}]
     ^{:key (str "cons-" x "-" y "-cdr-text")}
     [:text {:x (+ text-x box-width) :y text-y :fill "green"} cdr-value]

     ^{:key (str "cons-" x "-" y "-address")}
     [:text {:x text-x :y (- text-y 18) :fill "red"} address]

     ^{:key (str "atom-name-" x "-" y "-address")}
     [:text {:x (+ text-x box-width) :y (- text-y 18) :fill "blue"}
      (or (get atom-names address)
          (get ml-addresses cdr-value)
          "")]
     )))

(defn- draw-pointers [graph]
  (let [boxes (zipmap (map :address (:boxes graph)) (:boxes graph))]
    (doall
     (->>
      (:pointers graph)
      (map
       (fn [{:keys [from-car from-cdr to]}]
         ^{:key (str "from-" from-car from-cdr "-to-" to)}
         [:line {:x1 (if from-car
                       (-> (get boxes from-car) :x x-scale pointer-x-offset)
                       (-> (get boxes from-cdr) :x (+ 1) x-scale pointer-x-offset))
                 :y1 (if from-car
                       (-> (get boxes from-car) :y y-scale pointer-y-offset)
                       (-> (get boxes from-cdr) :y y-scale pointer-y-offset))
                 :x2 (-> (get boxes to) :x x-scale pointer-x-offset)
                 :y2 (-> (get boxes to) :y y-scale pointer-y-offset)
                 :stroke "blue"
                 :stroke-width 1}]))))))
  
(defn box-and-pointer-panel [processor]
  (let [id ::box-and-pointer-inspector
        inspection-sources @(rf/subscribe [::subs/inspection-sources])
        address (-> inspection-sources
                    (get id)
                    :address)
        graph (layout-graph {:x 0 :y 0 :address address}
                            []
                            (:mem processor))]
    [:div
     [inspection-source-selector processor id]

     [:svg {:width "1000" :height "300"}
      (draw-pointers graph)
      (doall (map (fn [{:keys [x y car cdr address] :as _box}]
                    (svg-cons (x-scale x) (y-scale y)
                              (util/hex-word car)
                              (util/hex-word cdr)
                              (util/hex-word address)
                              (:mem processor)))
                  (:boxes graph)))]]))


