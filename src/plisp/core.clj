(ns plisp.core)

(defn register-op
  "Parse a register operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])" "OP" op))]
    (let [[_ op reg] (re-matches re line)]
      (if op
        {:op (keyword op) :n (read-string (str "0x" reg)) :bytes 1}))))

(defn immediate-op
  "Parse an immediate operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+#([0-9a-fA-F][0-9a-fA-F])" "OP" op))]
    (let [[_ op operand] (re-matches re line)]
      (if op
        {:op (keyword op) :immediate (read-string (str "0x" operand)) :bytes 2}))))

(defn parse-line
  "Parse a single line of assembly."
  [line]
  (or
   (register-op "LDN" line)       ; 0N
   (register-op "INC" line)       ; 1N
   (register-op "DEC" line)       ; 2N
   
   (register-op "LDA" line)       ; 4N
   (register-op "STR" line)       ; 5N
   
   (register-op "GLO" line)       ; 8N
   (register-op "GHI" line)       ; 9N
   (register-op "PLO" line)       ; AN
   (register-op "PHI" line)       ; BN

   (register-op "SEP" line)       ; DN
   (register-op "SEX" line)       ; EN

   (immediate-op "LDI" line)      ; F8
   ))

(defn layout [instructions start-address]
  (loop [addr start-address
         instructions instructions
         memory {}]
    (if (empty? instructions)
      memory
      (recur (+ addr (:bytes (first instructions)))
             (rest instructions)
             (assoc-in memory [addr] (first instructions))))))

(defn prog []
  (let [instructions
        [
         (parse-line "  LDI #60")
         (parse-line "  PHI 3")
         (parse-line "  LDI #07")
         (parse-line "  PLO 3")
         (parse-line "  SEP 3")
         (parse-line "  LDI #70")
         (parse-line "  PHI F")
         (parse-line "  LDI #09")
         (parse-line "  PLO F")
         ]]
    (layout instructions 0x6000)))

(defn reset
  "Initial state of the processor."
  [prog]
  {:D 0x00
   :P 0x0
   :X 0x0
   :R [0X6000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000]
   :mem {}
   :prog prog
   })


(defn interpret [processor effects]
  (loop [[target produce-value & rest] effects
         processor processor]
    (let [partial (assoc-in processor target (produce-value))]
      (if (empty? rest)
        partial
        (recur rest partial)))))

(defn instruction-fetch [processor]
  (let [pc (get-in processor [:R (:P processor)])
        instruction (get-in processor [:prog pc])]
    [instruction
     (when instruction
       (assoc-in processor
                 [:R (:P processor)]
                 (+ pc (:bytes instruction))))]))

(defn replace-lo [value byte]
  (bit-or (bit-and value 0xff00) byte))

(defn replace-hi [value byte]
  (bit-or (bit-and value 0x00ff) (* 0x100 byte)))

(defn dump-instruction [instruction]
  (print (:op instruction))
  (when (:n instruction) (print (format " %x" (:n instruction))))
  (when (:immediate instruction) (print (format " #%02x" (:immediate instruction))))
  (newline))


(defn dump-processor [processor]
  (print (format "D: %02x  P: %x  X: %x\n"
                 (:D processor)
                 (:P processor)
                 (:X processor)))
  (dotimes [n 4]
    (print (format "R%x: %04x  R%x: %04x  R%x: %04x  R%x: %04x\n"
                   n (get-in processor [:R n])
                   (+ 4 n) (get-in processor [:R (+ 4 n)])
                   (+ 8 n) (get-in processor [:R (+ 8 n)])
                   (+ 12 n) (get-in processor [:R (+ 12 n)])))))
                             
(defn next-state [processor]
  (let [[instruction processor] (instruction-fetch processor)]
    (dump-instruction instruction)
    (when instruction
      (letfn
          [(P [] (:P processor))
           (D [] (:D processor))
           (R [n] (get-in processor [:R n]))
           ]
        (let [n (:n instruction)
              immediate (:immediate instruction)
              effect (case (:op instruction)
                       :PLO [[:R n]
                             (fn [] (replace-lo (R n) (D)))]
                       :PHI [[:R n]
                             (fn [] (replace-hi (R n) (D)))]
                       :SEP [[:P]
                             (fn [] n)]
                       :LDI [[:D]
                             (fn [] immediate)]
                       )
              final-state (when effect (interpret processor effect))]
          (when final-state (dump-processor final-state))
          final-state)))))

(defn run []
  (let [processor (reset (prog))]
    (dump-processor processor)
    (loop [processor processor]
      (let [next (next-state processor)]
        (when next (recur next))))))
