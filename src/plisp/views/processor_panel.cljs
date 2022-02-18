(ns plisp.views.processor-panel
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent-mui.material.button :refer [button]]
   [plisp.asm.lisp :as lisp]
   [plisp.services.processor-service :as processor-service]
   [plisp.subs :as subs]))

(defn hex-digit [n]
  (.toString n 16))

(defn- hex [byte]
  (str (when (< byte 0xf) "0")
       (.toString byte 16)))

(defn- register-cell [processor n]
  ^{:key (str "register-cell-" n)}
  [:td
   [:tt "R" (.toString n 16) ": "
    (hex (get-in processor [:R n :hi]))
    (hex (get-in processor [:R n :lo]))]])

(defn- register-row [processor n]
  ^{:key (str "register-row-" n)}
  [:tr
   (map (partial register-cell processor)
        [n (+ n 4) (+ n 8) (+ n 12)])])

(defn- source-line [base-source-line i line]
  (let [addr (get lisp/lisp-debug-info (+ base-source-line i))]
    (str
     (.toString addr 16)
     (if (zero? i)
       " =>"
       "   ")
   line)))

(defn- source-panel [current-instruction]
  [:pre
   (when current-instruction
     (->> lisp/lisp-source-text
          (drop (:source-line-number current-instruction))
          (take 5)
          (map-indexed (partial source-line (:source-line-number current-instruction)))
          (string/join "\n")))])

(defn processor-panel []
  (let [processor @(rf/subscribe [::subs/processor])
        instruction-count @(rf/subscribe [::subs/instruction-count])
        current-instruction @(rf/subscribe [::subs/current-instruction])]
    [:div
     [:div instruction-count " instructions executed"]
     [button {:on-click #(rf/dispatch [::processor-service/run-processor-tick 1])} "Step"]
     [:hr]
     [:table
      [:tbody
       [:tr
        [:td [:tt "D: " (hex (:D processor))]]
        [:td [:tt "DF: " (hex-digit (:DF processor))]]
        [:td [:tt "X: " (hex-digit (:X processor))]]
        [:td [:tt "P: " (hex-digit (:P processor))]]]
       (map (partial register-row processor) (range 4))]]
     [:hr]
     [:div "Input buffer: " (str (:input-buffer processor))]
     [:hr]
     [source-panel current-instruction]
     [:hr]]))
