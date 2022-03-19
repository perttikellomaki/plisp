(ns plisp.util
  (:require [clojure.string :as str])
  (:import goog.string))

(defn reg16 [x]
  (let [int16 (bit-and 0xffff x)]
    {:hi (quot int16 0x100) :lo (bit-and int16 0xff)}))

(defn int16 [reg]
  (+ (* (:hi reg) 256) (:lo reg)))


(defn event-value
  [e]
  (.. e -target -value))

(defn hex-string? [s]
  (every? (set "0123456789aAbBcCdDeEfF") s))

(defn str->html [s]
  (goog.string/unescapeEntities (apply str (map #(str/replace % " " "&nbsp;") s))))

(defn hex-digit [n]
  (.toString n 16))

(defn- hex-string [value n]
  (->> (.toString value 16)
       (str "0000")
       (take-last n)
       (apply str)))

(defn hex-byte [byte]
  (hex-string byte 2))

(defn hex-word [word]
  (hex-string word 4))

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
