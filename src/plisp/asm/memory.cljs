(ns plisp.asm.memory)

;;;
;;; Lay out instructions in memory.
;;;

(defn layout
  "Lay out instructions in memory. Returns a map representing the memory."
  [instructions]
  (loop [addr 0
         instructions instructions
         memory {}]
    (if (empty? instructions)
      memory
      (let [[insn & insns] instructions
            op (:op insn)]
        (cond (= op :address)
              (recur (:address insn)
                     insns
                     memory)
              (= op :string)
              (recur addr
                     (concat (map (fn [c] {:op :byte :value (.charCodeAt c) :bytes 1})
                                  (:value insn))
                             insns)
                     memory)
              (= op :empty)
              (recur addr
                     insns
                     memory)
              :else
              (recur (+ addr (:bytes insn))
                     insns
                     (assoc-in memory [addr] insn)))))))
