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

