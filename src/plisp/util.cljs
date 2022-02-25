(ns plisp.util)

(defn reg16 [x]
  (let [int16 (bit-and 0xffff x)]
    {:hi (quot int16 0x100) :lo (bit-and int16 0xff)}))

(defn int16 [reg]
  (+ (* (:hi reg) 256) (:lo reg)))


(defn event-value
  [e]
  (.. e -target -value))
