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
   [plisp.util :refer [event-value int16]]))

(defn hex-digit [n]
  (.toString n 16))

(defn- hex-string [value n]
  (->> (.toString value 16)
       (str "0000")
       (take-last n)
       (apply str)))

(defn- hex-byte [byte]
  (hex-string byte 2))

(defn- hex-word [word]
  (hex-string word 4))

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
  (str
   (.toString addr 16)
   (if (= i 0)
     " =>"
     "   ")
   line))

(defn- source-panel [{:keys [source-line-number] :as current-instruction}]
  (let [relative-window (range -4 10)
        window (->> (map #(+ source-line-number %) relative-window)
                    (take-while #(< % (count lisp/lisp-source-text))))
        ;; Each line is a vector [i address source-text]
        source-lines (map vector
                          (take (count window) relative-window)
                          (map #(get lisp/lisp-debug-info %)
                               window)
                          (map #(nth lisp/lisp-source-text %)
                               window))]
     [:pre
      (when current-instruction
        (->> (map format-source-line source-lines)
             (string/join "\n")))]))

(defn- address-window [offset-low address offset-high]
  (let [n (+ offset-low offset-high)]
    (cond
      (< (- address offset-low) 0)
      (let [relative-window (range (- address) (+ address n))
            window          (take n (range))]
        (map vector relative-window window))

      (> (+ address offset-high) 0xffff)
      (let [relative-window (range (- 0x10000 address n) (- 0x10000 address))
            window          (range (- 0x10000 offset-low offset-high)
                                   0x10000)]
        (map vector relative-window window))

      :else
      (let [relative-window (range (- offset-low) offset-high)
            window          (range (- address offset-low) (+ address offset-high))]
        (map vector relative-window window)))))

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

(defn- format-inspection-line [processor [i address]]
  (str (if (zero? i) "==> " "    ")
       (hex-word address)
       " "
       (let [{:keys [op value] :as memory-value} (get (:mem processor) address)]
         (cond
           (= op :byte)        (str (hex-byte value)
                                    " "
                                    (str (char value)))
           (nil? memory-value) "00"
           :else               "??"))))

(def select-state (reagent/atom "R0"))

(defn- inspection-panel [processor]
  (let [address (-> (get-in processor (register-path @select-state))
                    int16)
        window (address-window 3 address 8)]
    [:div
     [:div
     [text-field
      {:value       @select-state
       :label       "Select"
       :placeholder "Placeholder"
       :on-change   (fn [e]
                      (reset! select-state (event-value e)))
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
     [:pre
      (->> (map (partial format-inspection-line processor) window)
          (string/join "\n"))]]))


(defn processor-panel []
  (let [processor @(rf/subscribe [::subs/processor])
        instruction-count @(rf/subscribe [::subs/instruction-count])
        current-instruction @(rf/subscribe [::subs/current-instruction])]
    [:div
     [button {:on-click #(rf/dispatch [::processor-service/run-processor-tick 1])} "Step"]
     [:hr]
     [stack {:direction :row}
      [:div [registers processor]]
      [:div [source-panel current-instruction]]]
     [:div instruction-count " instructions executed"]
     [:hr]
     [:div "Input buffer: " (str (:input-buffer processor))]
     [:hr]
     [inspection-panel processor]]))
