(ns plisp.core)
(use 'clojure.data)

(defn no-operand-op
  "Parse a single byte operation with no register part."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s*(;.*)*" "OP" op))]
    (let [[_ op] (re-matches re line)]
      (if op
        {:op (keyword op) :bytes 1}))))

(defn register-op
  "Parse a register operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s*(;.*)*" "OP" op))]
    (let [[_ op reg] (re-matches re line)]
      (if op
        {:op (keyword op) :n (read-string (str "0x" reg)) :bytes 1}))))

(defn immediate-op
  "Parse an immediate operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+#([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))]
    (let [[_ op operand] (re-matches re line)]
      (if op
        {:op (keyword op) :immediate (read-string (str "0x" operand)) :bytes 2}))))

(defn register-immediate-op
  "Parse a register immediate operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+#([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))]
    (let [[_ op reg operand] (re-matches re line)]
      (if op
        {:op (keyword op) :n (read-string (str "0x" reg)) :long-immediate (read-string (str "0x" operand)) :bytes 4}))))

(defn subroutine-call-op
  "Parse a subroutine call instruction."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg address] (re-matches re line)]
    (if op
      {:op (keyword op) :n (read-string (str "0x" reg)) :long-address (read-string (str "0x" address)) :bytes 4})))

(defn short-branch-op
  "Parse a short branch operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))]
    (let [[_ op address] (re-matches re line)]
      (if op
        {:op (keyword op) :page-address (read-string (str "0x" address)) :bytes 2}))))

(defn address-directive
  "Parse an address directive."
  [line]
  (let [[_ address] (re-matches #"\s*([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]):\s*(;.*)*" line)]
    (if address
      {:op :address :address (read-string (str "0x" address))})))

(defn byte-directive
  "Parse a byte directive."
  [line]
  (let [[_ byte] (re-matches #"\s*BYTE\s+#([0-9a-fA-F][0-9a-fA-F])\s*(;.*)*" line)]
    (if byte
      {:op :byte :value (read-string (str "0x" byte)) :bytes 1})))

(defn empty-line
  "Parse a comment line."
  [line]
  (if (re-matches #"\s*(;.*)*" line)
    {:op :empty :bytes 0}))


(defn parse-line
  "Parse a single line of assembly."
  [line]
  (or
   (address-directive line)
   (byte-directive line)
   (empty-line line)
   (no-operand-op "NOP" line)
   (register-op "LDN" line)
   (register-op "INC" line)
   (register-op "DEC" line)
   (short-branch-op "BR" line)
   (short-branch-op "BNZ" line)
   (register-op "LDA" line)
   (register-op "STR" line)
   (register-op "GLO" line)
   (register-op "GHI" line)
   (register-op "PLO" line)
   (register-op "PHI" line)
   (register-op "SEP" line)
   (register-op "SEX" line)
   (immediate-op "LDI" line)
   (immediate-op "XRI" line)
   (register-immediate-op "RLDI" line)
   (subroutine-call-op "SCAL" line)
   ))

(defn layout [instructions]
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
              (= op :byte)
              (recur (+ addr 1)
                     insns
                     (assoc-in memory [addr] (:value insn)))
              (= op :empty)
              (recur addr
                     insns
                     memory)
              :else
              (recur (+ addr (:bytes insn))
                     insns
                     (assoc-in memory [addr] insn)))))))

(defn prog []
   (with-open [rdr (clojure.java.io/reader "lisp.asm")]
     (layout
      (map parse-line (reduce conj [] (line-seq rdr))))))

(defn reset
  "Initial state of the processor."
  [prog]
  {:D 0x00
   :P 0x0
   :X 0x0
   :R [0X6000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000]
   :mem prog
   })


(defn interpret [processor effects]
  (loop [effects effects
         partial processor]
    (if (empty? effects)
      partial
      (let [[target produce-value & rest] effects
            partial (assoc-in partial target (produce-value))]
        (recur rest partial)))))

(defn instruction-fetch [processor]
  (let [pc (get-in processor [:R (:P processor)])
        instruction (get-in processor [:mem pc])]
    [instruction
     (when instruction
       (assoc-in processor
                 [:R (:P processor)]
                 (+ pc (:bytes instruction))))]))

(defn inc-16bit [value]
  (bit-and 0xffff (+ value 1)))

(defn dec-16bit [value]
  (bit-and 0xffff (- value 1)))

(defn get-lo [value]
  (bit-and value 0xff))

(defn get-hi [value]
  (quot (bit-and value 0xff00) 0x100))

(defn replace-lo [value byte]
  (bit-or (bit-and value 0xff00) byte))

(defn replace-hi [value byte]
  (bit-or (bit-and value 0x00ff) (* 0x100 byte)))

(defn short-branch [page-address condition pc]
  (if condition
    (replace-lo pc page-address)
    pc))

(defn dump-instruction [instruction]
  (print (:op instruction))
  (when (:n instruction) (print (format " %x" (:n instruction))))
  (when (:immediate instruction) (print (format " #%02x" (:immediate instruction))))
  (when (:long-immediate instruction) (print (format " #%04x" (:long-immediate instruction))))
  (when (:page-address instruction) (print (format " %02x" (:page-address instruction))))
  (when (:long-address instruction) (print (format " %04x" (:long-address instruction))))
  (newline))


(defn dump-processor [previous processor]
  (print (format "D: %02x  P: %x  X: %x\n"
                 (:D processor)
                 (:P processor)
                 (:X processor)))
  (dotimes [n 4]
    (print (format "R%x: %04x  R%x: %04x  R%x: %04x  R%x: %04x\n"
                   n (get-in processor [:R n])
                   (+ 4 n) (get-in processor [:R (+ 4 n)])
                   (+ 8 n) (get-in processor [:R (+ 8 n)])
                   (+ 12 n) (get-in processor [:R (+ 12 n)]))))
  (let [[_ changes _]  (diff previous processor)]
    (when (:mem changes)
      (doseq [[addr val] (:mem changes)]
        (print (format "mem %04x: %02x\n" addr val))))))


(defn next-state [processor]
  (let [[instruction processor] (instruction-fetch processor)]
    (dump-instruction instruction)
    (when instruction
      (letfn
          [(P [] (:P processor))
           (X [] (:X processor))
           (D [] (:D processor))
           (mem [addr]
             (print (format "%04x: %02x"
                            addr
                            (get-in processor [:mem addr])))
             (newline)
             (get-in processor [:mem addr]))
           (R [n] (get-in processor [:R n]))
           ]
        (let [n (:n instruction)
              immediate (:immediate instruction)
              long-immediate (:long-immediate instruction)
              page-address (:page-address instruction)
              long-address (:long-address instruction)
              effect (case (:op instruction)
                       :NOP []
                       :INC [[:R n]
                             (fn [] (inc-16bit (R n)))]
                       :BR  [[:R (P)]
                             (fn [] (short-branch page-address true (R (P))))]
                       :BNZ [[:R (P)]
                             (fn [] (short-branch page-address (not= (D) 0) (R (P))))]
                       :LDA [[:D]
                             (fn [] (mem (R n)))
                             [:R n]
                             (fn [] (inc-16bit (R n)))]
                       :STR [[:mem (R n)]
                             (fn [] (D))]
                       :GLO [[:D]
                             (fn [] (get-lo (R n)))]
                       :GHI [[:D]
                             (fn [] (get-hi (R n)))]
                       :PLO [[:R n]
                             (fn [] (replace-lo (R n) (D)))]
                       :PHI [[:R n]
                             (fn [] (replace-hi (R n) (D)))]
                       :SEP [[:P]
                             (fn [] n)]
                       :SEX [[:X]
                             (fn [] n)]
                       :LDI [[:D]
                             (fn [] immediate)]
                       :XRI [[:D]
                             (fn [] (bit-xor (D) immediate))]
                       :RLDI [[:R n]
                              (fn [] long-immediate)]
                       :SCAL [[:mem (R (X))]
                              (fn [] (get-lo (R n)))
                              [:mem (dec-16bit (R (X)))]
                              (fn [] (get-hi (R n)))
                              [:R (X)]
                              (fn [] (dec-16bit (dec-16bit (R (X)))))
                              [:R n]
                              (fn [] (R (P)))
                              [:R (P)]
                              (fn [] long-address)]
                       )
              final-state (when effect (interpret processor effect))]
          (when final-state (dump-processor processor final-state))
          final-state)))))

(defn run []
  (let [processor (reset (prog))]
    (dump-processor processor processor)
    (loop [processor processor]
      (let [next (next-state processor)]
        (when next (recur next))))))
