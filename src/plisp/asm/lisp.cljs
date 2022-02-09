(ns plisp.asm.lisp
  (:require
   [clojure.string :as str]
   [shadow.resource :as rc]
   [plisp.asm.memory :as memory]
   [plisp.asm.parser :as parser]
   [plisp.cosmac.processor :as processor]
))

(def lisp-source-text
  (-> (rc/inline "./lisp.asm")
      (str/split #"\n")))

(def lisp-source
  (->> (map parser/parse-line lisp-source-text)
      (filter #(not= (:op %) :empty))))


;;;
;;; An execution is a potentially infinite sequence of processor states.
;;;

(defn execution [prog input]
  (iterate processor/next-state
           (processor/reset prog 0x6000 input)))

(def initial-lisp-memory (memory/layout lisp-source))

;;;
;;; Run Lisp.
;;;

(defn lisp-execution [input]
  (execution initial-lisp-memory
             (concat input ["\r" "\004"])))

(defn run-lisp [input]
  (apply str
         (-> (drop-while #(= (:status %) :running) (lisp-execution input))
             first
             :output-buffer)))
