(ns plisp.core)
(use 'clojure.data)

(def trace-options
  [
   ; :instruction
   ; :processor
   ; :memory
   ])

;;;
;;; Parsers for lines of assembly.
;;;

(defn no-operand-op
  "Parse a single byte operation with no register part."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s*(;.*)*" "OP" op))
        [_ op] (re-matches re line)]
    (if op
      {:op (keyword op) :bytes 1})))

(defn register-op
  "Parse a register operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg] (re-matches re line)]
    (if op
      {:op (keyword op) :n (read-string (str "0x" reg)) :bytes 1})))

(defn immediate-op
  "Parse an immediate operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+#([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op operand] (re-matches re line)]
    (if op
      {:op (keyword op) :immediate (read-string (str "0x" operand)) :bytes 2})))

(defn register-immediate-op
  "Parse a register immediate operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+#([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg operand] (re-matches re line)]
    (if op
      {:op (keyword op) :n (read-string (str "0x" reg)) :long-immediate (read-string (str "0x" operand)) :bytes 4})))

(defn subroutine-call-op
  "Parse a subroutine call instruction."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg address] (re-matches re line)]
    (if op
      {:op (keyword op) :n (read-string (str "0x" reg)) :long-address (read-string (str "0x" address)) :bytes 4})))

(defn subroutine-return-op
  "Parse a subroutine call instruction."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg] (re-matches re line)]
    (if op
      {:op (keyword op) :n (read-string (str "0x" reg)) :bytes 2})))

(defn short-branch-op
  "Parse a short branch operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op address] (re-matches re line)]
    (if op
      {:op (keyword op) :page-address (read-string (str "0x" address)) :bytes 2})))

(defn long-branch-op
  "Parse a long branch operation."
  [op line]
  (let [re (re-pattern (clojure.string/replace "\\s*(OP)\\s+([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op address] (re-matches re line)]
    (if op
      {:op (keyword op) :long-address (read-string (str "0x" address)) :bytes 3})))

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

(defn string-directive
  "Parse a string directive."
  [line]
  (let [[_ str] (re-matches #"\s*STRING\s+\"([^\"]*)\"\s*(;.*)*" line)]
    (if str
      {:op :string :value str :bytes (count str)})))

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
   (string-directive line)
   (empty-line line)
   (no-operand-op "IDLE" line)
   (no-operand-op "NOP" line)
   (no-operand-op "XOR" line)
   (register-op "LDN" line)
   (register-op "INC" line)
   (register-op "DEC" line)
   (short-branch-op "BR" line)
   (short-branch-op "BZ" line)
   (short-branch-op "BNZ" line)
   (long-branch-op "LBR" line)
   (register-op "LDA" line)
   (register-op "STR" line)
   (register-op "GLO" line)
   (register-op "GHI" line)
   (register-op "PLO" line)
   (register-op "PHI" line)
   (register-op "SEP" line)
   (register-op "SEX" line)
   (immediate-op "LDI" line)
   (immediate-op "ADI" line)
   (immediate-op "SMI" line)
   (immediate-op "ORI" line)
   (immediate-op "XRI" line)
   (register-immediate-op "RLDI" line)
   (subroutine-call-op "SCAL" line)
   (subroutine-return-op "SRET" line)

   ;; pseudo ops
   (no-operand-op "PRINTCHAR" line)
   ))

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
                     (concat (map (fn [c] {:op :byte :value (int c) :bytes 1})
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

;;;
;;; Read assembly program from file lisp.asm.
;;;

(defn prog
  "Assembly program from lisp.asm."
  []
  (with-open [rdr (clojure.java.io/reader "lisp.asm")]
    (layout
     (map parse-line (reduce conj [] (line-seq rdr))))))


;;;
;;; Default reader and writer.
;;;

(defn writer-impl [cumul]
  (fn
    ([] (clojure.string/join cumul))
    ([c]
     (print c)
     (writer-impl (conj cumul c)))))

(defn default-writer
  "The default writer writes one character to stdout and returns a new writer.
  Without argument dumps a string containing all characters written so far."
  []
  (writer-impl []))

;;;
;;; The processor state.
;;;

(defn reset
  "Initial state of the processor. Optionally with starting address in R0."
  ([prog] (reset prog 0x0000))
  ([prog start-addr] (reset prog start-addr (default-writer)))
  ([prog start-addr writer]
   {:D 0x00
    :DF 0
    :P 0x0
    :X 0x0
    :R [start-addr 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000]
    :mem prog
    :writer writer
    :running true
    }))

;;;
;;; Execute an instruction.
;;; Each effect consists of a target suitable for assoc-in,
;;; and a thunk yielding the new value of the target.
;;; 

(defn execute-instruction
  "Execute an instruction by applying effects to procesor state."
  [processor effects]
  (loop [effects effects
         partial processor]
    (if (empty? effects)
      partial
      (let [[target produce-value & rest] effects
            partial (assoc-in partial target (produce-value))]
        (recur rest partial)))))

;;;
;;; Fetch an instruction.
;;; Returns the instruction and new processor state.
;;;

(defn instruction-fetch
  "Fetch an instruction from R(P) and advance R(P)."
  [processor]
  (let [pc (get-in processor [:R (:P processor)])
        instruction (get-in processor [:mem pc])]
    [instruction
     (when instruction
       (assoc-in processor
                 [:R (:P processor)]
                 (+ pc (:bytes instruction))))]))

;;;
;;; Helpers for implementing instuction semantics.
;;;

(defn mem-byte [val] {:op :byte :value val})

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

;;;
;;; Dumping of instructions and processor state.
;;;

(defn dump-instruction
  "Dump a single instruction."
  [instruction]
  (print (:op instruction))
  (when (:n instruction) (print (format " %x" (:n instruction))))
  (when (:immediate instruction) (print (format " #%02x" (:immediate instruction))))
  (when (:long-immediate instruction) (print (format " #%04x" (:long-immediate instruction))))
  (when (:page-address instruction) (print (format " %02x" (:page-address instruction))))
  (when (:long-address instruction) (print (format " %04x" (:long-address instruction))))
  (newline))


(defn dump-processor
  "Dump processor state."
  [previous processor]
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
        (print (format "mem %04x: %02x\n" addr (:value val)))))))

(defn dump-address
  "Dump memory address."
  [addr-val]
  (let [[addr val] addr-val]
    (format "%04x: %s" addr val)))

(defn lst
  "List memory contents."
  [memory]
  (map dump-address (sort memory)))

;;;
;;; The processor simulator proper.
;;; Each instruction is described as a list of effects on processor state.
;;; Each effect consists of a sequence of keys which specifies a location
;;; in processor state, and a parameterless function which produces the
;;; new value. For example, immediate XOR instruction is specified as:
;;;
;;;       :XRI [[:D]
;;;             (fn [] (bit-xor (D) immediate))]
;;;
;;; The [:D] tells that the D register (accumulator) is to be changed,
;;; and the new value is given by xoring the current D register with the
;;; immediate byte following the opcode.
;;;
;;; Function next-state returns the new processor state after
;;; executing a single instruction.
;;;

(defn next-state
  "Return the next state of the processor after executing one instruction."
  [processor]
  (let [[instruction processor] (instruction-fetch processor)]
    (when (some #{:instruction} trace-options)
      (dump-instruction instruction))
    (when instruction
      (letfn
          [(P [] (:P processor))
           (X [] (:X processor))
           (D [] (:D processor))
           (mem [addr]
             (when (some #{:memory} trace-options)
               (print (format "%04x: %02x"
                              addr
                              (:value (get-in processor [:mem addr]))))
               (newline))
             (:value (get-in processor [:mem addr])))
           (R [n] (get-in processor [:R n]))]
        (let [n (:n instruction)
              immediate (:immediate instruction)
              long-immediate (:long-immediate instruction)
              page-address (:page-address instruction)
              long-address (:long-address instruction)
              effect (case (:op instruction)
                       :IDLE [[:running]
                              (fn [] false)]
                       :NOP []
                       :INC [[:R n]
                             (fn [] (inc-16bit (R n)))]
                       :BR  [[:R (P)]
                             (fn [] (short-branch page-address true (R (P))))]
                       :BZ [[:R (P)]
                             (fn [] (short-branch page-address (= (D) 0) (R (P))))]
                       :BNZ [[:R (P)]
                             (fn [] (short-branch page-address (not= (D) 0) (R (P))))]
                       :LBR [[:R (P)]
                             (fn [] long-address)]
                       :LDN [[:D]
                             (fn [] (mem (R n)))]
                       :LDA [[:D]
                             (fn [] (mem (R n)))
                             [:R n]
                             (fn [] (inc-16bit (R n)))]
                       :STR [[:mem (R n)]
                             (fn [] (mem-byte (D)))]
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
                       :XOR [[:D]
                             (fn [] (bit-xor (D) (mem (R (X)))))]
                       :ADI [[:D]
                             (fn [] (bit-and 0xff (+ (D) immediate)))
                             [:DF]
                             (fn [] (if (> (+ (D) immediate) 0xff) 1 0))]
                       :SMI [[:D]
                             (fn [] (bit-and 0xff (- (D) immediate)))
                             [:DF]
                             (fn [] (if (>= (D) immediate) 1 0))]
                       :ORI [[:D]
                             (fn [] (bit-or (D) immediate))]
                       :XRI [[:D]
                             (fn [] (bit-xor (D) immediate))]
                       :RLDI [[:R n]
                              (fn [] long-immediate)]
                       :SCAL [[:mem (R (X))]
                              (fn [] (mem-byte (get-lo (R n))))
                              [:mem (dec-16bit (R (X)))]
                              (fn [] (mem-byte (get-hi (R n))))
                              [:R (X)]
                              (fn [] (dec-16bit (dec-16bit (R (X)))))
                              [:R n]
                              (fn [] (R (P)))
                              [:R (P)]
                              (fn [] long-address)]
                       :SRET [[:R (P)]
                              (fn [] (R n))
                              [:R n]
                              (fn [] (+ (* (mem (inc-16bit (R (X)))) 0x100)
                                        (mem (inc-16bit (inc-16bit (R (X)))))))
                              [:R (X)]
                              (fn [] (inc-16bit (inc-16bit (R (X)))))]
                       :PRINTCHAR [[:writer]
                                   (fn [] ((:writer processor) (char (D))))]

                       ;; Just enough support for executing hex coded instructions
                       ;; to get the Lisp running.
                       :byte [[:R (P)]
                              (fn []
                                (if (= (:value instruction) 0xc0)
                                  (+ (* (mem (R (P))) 0x100)
                                     (mem (inc-16bit (R (P)))))
                                  (R (P)))) ; silent NOP
                              ]
                       )
              final-state (when effect (execute-instruction processor effect))]
          (when (and final-state
                     (some #{:processor} trace-options))
            (dump-processor processor final-state))
          final-state)))))

;;;
;;; Run Lisp.
;;;

(defn run []
  (let [processor (reset (prog) 0x6000)]
    (when (some #{:processor} trace-options)
      (dump-processor processor processor))
    (loop [processor processor]
      (let [next (next-state processor)]
        (if (:running next)
          (recur next)
          next)))))
