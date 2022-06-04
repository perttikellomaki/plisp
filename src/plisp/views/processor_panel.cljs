(ns plisp.views.processor-panel
  (:require
   [re-frame.core :as rf]
   [reagent-mui.material.stack :refer [stack]]
   [plisp.asm.lisp :as lisp]
   [plisp.subs :as subs]
   [plisp.util :refer [hex-digit hex-byte hex-word] :as util]
   [plisp.views.inspection-source-selector :refer [inspection-source-selector]]
   [plisp.views.box-and-pointer :as box-and-pointer]))

(defn- register-cell [processor n]
  ^{:key (str "register-cell-" n)}
  [:td {:style {:padding-right 30}}
   [:tt "R" (.toString n 16) ": "
    (hex-byte (get-in processor [:R n :hi]))
    (hex-byte (get-in processor [:R n :lo]))]])

(defn- register-row [processor n]
  ^{:key (str "register-row-" n)}
  [:tr
   (map (partial register-cell processor)
        [n (+ n 8)])])

(defn- registers [processor]
  [:table {:style {:min-width 200}}
   [:tbody
    [:tr
     [:td [:tt "D: " (hex-byte (:D processor))]]
     [:td [:tt "X: " (hex-digit (:X processor))]]]
    [:tr
     [:td [:tt "DF: " (hex-digit (:DF processor))]]
     [:td [:tt "P: " (hex-digit (:P processor))]]]
    (map (partial register-row processor) (range 8))]])

(defn- format-source-line [[i addr line]]
  ^{:key (str "source-" i)}
  [:tr
   [:td (if addr
          (.toString addr 16)
          "????")]
   [:td (when (= i 0) "=>")]
   [:td line]])

(defn- source-panel [{:keys [source-line-number] :as current-instruction}]
  (let [relative-window (range -3 8)
        window (->> (map #(+ source-line-number %) relative-window)
                    (take-while #(< % (count lisp/lisp-source-text))))
        ;; Each line is a vector [i address source-text]
        source-lines (map vector
                          (take (count window) relative-window)
                          (map #(get lisp/lisp-debug-info %)
                               window)
                          (map #(nth lisp/lisp-source-html %)
                               window))]
    (when current-instruction
      [:table
       [:tbody
        (map format-source-line source-lines)]])))

(defn- address-window [{:keys [address step rows-before rows-after]}]
  (let [zero-line (- address (mod address step))
        first-address (- zero-line (* step (inc rows-before)))
        last-address (+ zero-line (* step rows-after))
        lines (partition step step (range first-address last-address))]
    (map (fn [line] (map #(bit-and 0xffff %) line))
         lines)))

(defn- memory-as-byte [processor address]
  (let [{:keys [op value] :as memory-value} (get (:mem processor) address)]
    (cond
      (= op :byte) (hex-byte value)
      (nil? memory-value) "00"
      :else               "??")))

(defn- memory-as-char [processor address]
  (let [{:keys [op value]} (get (:mem processor) address)]
    (if (= op :byte) (char value)
        " ")))

(defn- format-inspection-line [processor address addresses]
  ^{:key (str "tr-" (first addresses))}
  [:tr
   [:td (when ((set addresses) address) "==>")]
   [:td (hex-word (first addresses))]
   (map (fn [addr]
          (let [prefix (if (= addr address) :b :span)]
            ^{:key (str "td-" addr)}
            [:td [prefix (memory-as-byte processor addr)]]))
        addresses)
   [:td (apply str
               (map (fn [addr]
                      (memory-as-char processor addr))
                    addresses))]])


(defn- inspection-panel [processor]
  (let [id  ::memory-inspector
        inspection-address @(rf/subscribe [::subs/inspection-address id])
        window (address-window {:address inspection-address
                                :step    4
                                :rows-before 3
                                :rows-after 8})]
    [:div
     [inspection-source-selector processor id]
     [:table
      [:tbody
       (map #(format-inspection-line processor inspection-address %) window)]]]))

(defn processor-panel []
  (let [processor @(rf/subscribe [::subs/processor])
        current-instruction @(rf/subscribe [::subs/current-instruction])]
    [:div {:style {:flex "1"
                   :overflow "auto"
                   :min-height 0}}
     [:hr]
     [stack {:direction :row}
      [:div [registers processor]]
      [:div [source-panel current-instruction]]]
     [:hr]
     [:div "Input buffer: " (str (:input-buffer processor))]
     [:hr]
     [:div {:style {:display "flex"
                   :flex-flow "row"
                   :overflow "hidden"
                   :height "100%"}}
      [inspection-panel processor]
      [box-and-pointer/box-and-pointer-panel processor]]]))
