(ns plisp.views.processor-panel
  (:require
   [clojure.string :as string]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.menu-item :refer [menu-item]]
   [reagent-mui.material.stack :refer [stack]]
   [reagent-mui.material.text-field :refer [text-field]]
   [plisp.asm.lisp :as lisp]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]
   [plisp.util :refer [event-value hex-digit hex-byte hex-word int16] :as util]))

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
   [:td (str (.toString addr 16))]
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

(defn register-path [selection]
  (case selection
    "R0" [:R 0x0]
    "R1" [:R 0x1]
    "R2" [:R 0x2]
    "R3" [:R 0x3]
    "R4" [:R 0x4]
    "R5" [:R 0x5]
    "R6" [:R 0x6]
    "R7" [:R 0x7]
    "R8" [:R 0x8]
    "R9" [:R 0x9]
    "Ra" [:R 0xa]
    "Rb" [:R 0xb]
    "Rc" [:R 0xc]
    "Rd" [:R 0xd]
    "Re" [:R 0xe]
    "Rf" [:R 0xf]))

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
  (let [inspection-source @(rf/subscribe [::subs/inspection-source])
        address (-> (get-in processor (register-path inspection-source))
                    int16)
        window (address-window {:address address
                                :step    4
                                :rows-before 3
                                :rows-after 8})]
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
     [:table
      [:tbody
       (map #(format-inspection-line processor address %) window)]]]))

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
     [inspection-panel processor]]))
