(ns plisp.core
  (:require [clojure.data :refer :all]))

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

(defn extended-register-op
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
   ;; 0x00
   (no-operand-op "IDLE" line)
   ;; 0x0N
   (register-op "LDN" line)
   ;; 0x1N
   (register-op "INC" line)
   ;; 0x2N
   (register-op "DEC" line)
   ;; 0x30
   (short-branch-op "BR" line)
   ;; 0x31
   ;; 0x32
   (short-branch-op "BZ" line)
   ;; 0x33
   ;; 0x34
   ;; 0x35
   ;; 0x36
   ;; 0x37
   ;; 0x38
   ;; 0x39
   ;; 0x3A
   (short-branch-op "BNZ" line)
   ;; 0x3B
   ;; 0x3C
   ;; 0x3D
   ;; 0x3E
   ;; 0x3F
   ;; 0x4N
   (register-op "LDA" line)
   ;; 0x5N
   (register-op "STR" line)
   ;; 0x60
   ;; 0x61
   ;; 0x62
   ;; 0x63
   ;; 0x64
   ;; 0x65
   ;; 0x66
   ;; 0x67
   ;; 0x68 6N
   (extended-register-op "RLXA" line)
   ;; 0x68 8N
   (subroutine-call-op "SCAL" line)
   ;; 0x68 9N
   (extended-register-op "SRET" line)
   ;; 0x68 AN
   (extended-register-op "RSXD" line)
   ;; 0x68 BN
   (extended-register-op "RNX" line)
   ;; 0x68 CN
   (register-immediate-op "RLDI" line)
   ;; 0x69
   ;; 0x6A
   ;; 0x6B
   ;; 0x6C
   ;; 0x6D
   ;; 0x6E
   ;; 0x6F
   ;; 0x70
   ;; 0x71
   ;; 0x72
   ;; 0x73
   (no-operand-op "STXD" line)
   ;; 0x74
   ;; 0x75
   ;; 0x76
   ;; 0x77
   ;; 0x78
   ;; 0x79
   ;; 0x7A
   ;; 0x7B
   ;; 0x7C
   (immediate-op "ADCI" line)
   ;; 0x7D
   ;; 0x7E
   ;; 0x7F
   (immediate-op "SMBI" line)
   ;; 0x8N
   (register-op "GLO" line)
   ;; 0x9N
   (register-op "GHI" line)
   ;; 0xAN
   (register-op "PLO" line)
   ;; 0xBN
   (register-op "PHI" line)
   ;; 0xC0
   (long-branch-op "LBR" line)
   ;; 0xC1
   ;; 0xC2
   ;; 0xC3
   ;; 0xC4
   (no-operand-op "NOP" line)
   ;; 0xC5
   ;; 0xC6
   (no-operand-op "LSNZ" line)
   ;; 0xC7
   ;; 0xC8
   ;; 0xC9
   ;; 0xCA
   ;; 0xCB
   ;; 0xCC
   ;; 0xCD
   ;; 0xCE
   ;; 0xCF
   ;; 0xDN
   (register-op "SEP" line)
   ;; 0xEN
   (register-op "SEX" line)
   ;; 0xF0
   ;; 0xF1
   (no-operand-op "OR" line)
   ;; 0xF2
   ;; 0xF3
   (no-operand-op "XOR" line)
   ;; 0xF4
   (no-operand-op "ADD" line)
   ;; 0xF5
   ;; 0xF6
   ;; 0xF7
   ;; 0xF8
   (immediate-op "LDI" line)
   ;; 0xF9
   (immediate-op "ORI" line)
   ;; 0xFA
   (immediate-op "ANI" line)
   ;; 0xFB
   (immediate-op "XRI" line)
   ;; 0xFC
   (immediate-op "ADI" line)
   ;; 0xFD
   ;; 0xFE
   ;; 0xFF
   (immediate-op "SMI" line)

   ;; pseudo ops
   (no-operand-op "PRINTCHAR" line)
   (no-operand-op "READCHAR" line)
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
;;; Default reader.
;;;

(defn reader-impl [chars]
  (fn [] (let [[c & cs] chars] [c (reader-impl cs)])))

(defn reader [input]
  (reader-impl (seq input)))

;;;
;;; The processor state.
;;;
;;; Output is modeled with :outchar, which contains an output character
;;; after a PRINTCHAR pseudo instruction, and nil otherwise.
;;;

(defn reset
  "Initial state of the processor. Optionally with starting address in R0."
  ([prog] (reset prog 0x0000))
  ([prog start-addr] (reset prog start-addr (reader "")))
  ([prog start-addr reader]
   {:D 0x00
    :DF 0
    :P 0x0
    :X 0x0
    :R [start-addr 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000]
    :mem prog
    :reader reader
    :outchar nil
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
  ([initial-processor]
   (let [addr (get-in initial-processor [:R (:P initial-processor)])
         [instruction processor] (instruction-fetch initial-processor)]
     (when instruction
       (letfn
           [(P [] (:P processor))
            (X [] (:X processor))
            (D [] (:D processor))
            (DF [] (:DF processor))
            (mem [addr] 
              ;; assume uninitialized memory is zeroed out
              (or (:value (get-in processor [:mem addr]))
                  0x00))
            (R [n] (get-in processor [:R n]))]
         (let [{:keys [n immediate long-immediate page-address long-address]} instruction]
           (let [effect (case (:op instruction)
                          :IDLE [[:running]
                                 (fn [] false)]
                          :LDN [[:D]
                                (fn [] (mem (R n)))]
                          :INC [[:R n]
                                (fn [] (inc-16bit (R n)))]
                          :DEC [[:R n]
                                (fn [] (dec-16bit (R n)))]
                          :BR  [[:R (P)]
                                (fn [] (short-branch page-address true (R (P))))]
                          :BZ [[:R (P)]
                               (fn [] (short-branch page-address (= (D) 0) (R (P))))]
                          :BNZ [[:R (P)]
                                (fn [] (short-branch page-address (not= (D) 0) (R (P))))]
                          :LDA [[:D]
                                (fn [] (mem (R n)))
                                [:R n]
                                (fn [] (inc-16bit (R n)))]
                          :STR [[:mem (R n)]
                                (fn [] (mem-byte (D)))]
                          :RLXA [[:R (X)]
                                 (fn [] (inc-16bit (inc-16bit (R (X)))))
                                 [:R n]
                                 (fn [] (+ (* (mem (R (X))) 0x100)
                                           (mem (inc-16bit (R (X))))))]
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
                          :RSXD [[:mem (R (X))]
                                 (fn [] (mem-byte (get-lo (R n))))
                                 [:mem (dec-16bit (R (X)))]
                                 (fn [] (mem-byte (get-hi (R n))))
                                 [:R (X)]
                                 (fn [] (dec-16bit (dec-16bit (R (X)))))]
                          :RNX [[:R (X)]
                                (fn [] (R n))]
                          :RLDI [[:R n]
                                 (fn [] long-immediate)]
                          :STXD [[:mem (R (X))]
                                 (fn [] (mem-byte (D)))
                                 [:R (X)]
                                 (fn [] (dec-16bit (R (X))))]
                          :ADCI [[:D]
                                 (fn [] (bit-and 0xff (+ (D) (DF) immediate)))
                                 [:DF]
                                 (fn [] (if (> (+ (D) (DF) immediate) 0xff) 1 0))]
                          :SMBI [[:D]
                                 (fn [] (bit-and
                                         0xff
                                         (- (D)
                                            immediate
                                            (if (= (DF) 0) 1 0))))
                                 [:DF]
                                 (fn [] (if (>= (- (D)
                                                   immediate
                                                   (if (= (DF) 0) 1 0))
                                                0)
                                          1
                                          0))]
                          :GLO [[:D]
                                (fn [] (get-lo (R n)))]
                          :GHI [[:D]
                                (fn [] (get-hi (R n)))]
                          :PLO [[:R n]
                                (fn [] (replace-lo (R n) (D)))]
                          :PHI [[:R n]
                                (fn [] (replace-hi (R n) (D)))]
                          :LBR [[:R (P)]
                                (fn [] long-address)]
                          :NOP []
                          :LSNZ [[:R (P)]
                                 (fn [] (if (not= (D) 0)
                                          (inc-16bit (inc-16bit (R (P))))
                                          (R (P))))]
                          :SEP [[:P]
                                (fn [] n)]
                          :SEX [[:X]
                                (fn [] n)]
                          :OR [[:D]
                               (fn [] (bit-or (D) (mem (R (X)))))]
                          :XOR [[:D]
                                (fn [] (bit-xor (D) (mem (R (X)))))]
                          :ADD [[:D]
                                (fn [] (bit-and 0xff (+ (D) (mem (R (X))))))
                                [:DF]
                                (fn [] (if (> (+ (D) (mem (R (X)))) 0xff) 1 0))]
                          :LDI [[:D]
                                (fn [] immediate)]
                          :ORI [[:D]
                                (fn [] (bit-or (D) immediate))]
                          :ANI [[:D]
                                (fn [] (bit-and (D) immediate))]
                          :XRI [[:D]
                                (fn [] (bit-xor (D) immediate))]
                          :ADI [[:D]
                                (fn [] (bit-and 0xff (+ (D) immediate)))
                                [:DF]
                                (fn [] (if (> (+ (D) immediate) 0xff) 1 0))]
                          :SMI [[:D]
                                (fn [] (bit-and 0xff (- (D) immediate)))
                                [:DF]
                                (fn [] (if (>= (D) immediate) 1 0))]
                          :PRINTCHAR [[:outchar]
                                      (fn [] (char (D)))]
                          :READCHAR [[:D]
                                     (fn [] (let [[c r] ((:reader processor))] (int c)))
                                     [:reader]
                                     (fn [] (let [[c r] ((:reader processor))] r))]

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
                 final-state (when effect
                               (let [next (execute-instruction processor effect)]
                                 ;; clear outchar if instruction not PRINTCHAR
                                 (if (= (:op instruction) :PRINTCHAR)
                                   next
                                   (assoc-in next [:outchar] nil))))]
             final-state)))))))

;;;
;;; An execution is a potentially infinite sequence of processor states.
;;;

(defn execution [prog reader]
  (iterate next-state (reset prog 0x6000 reader)))

;;;
;;; Run Lisp.
;;;

(defn run-lisp [input]
  (apply str
         (->> (take-while :running (execution (prog) (reader (str input "\r\004"))))
              (map :outchar))))

;;;
;;; Dumping of instructions and processor state.
;;;

(defn format-instruction
  "Format a single instruction."
  [instruction]
  (str
   (:op instruction)
   (when (:n instruction) (format " %x" (:n instruction)))
   (when (:immediate instruction) (format " #%02x" (:immediate instruction)))
   (when (:long-immediate instruction) (format " #%04x" (:long-immediate instruction)))
   (when (:page-address instruction) (format " %02x" (:page-address instruction)))
   (when (:long-address instruction) (format " %04x" (:long-address instruction)))
   (when (:value instruction) (format " %02x %s" (:value instruction) (char (:value instruction))))))

(defn dump-instruction
  "Dump a single instruction."
  [instruction]
  (println (format-instruction instruction)))

(defn dump-processor
  "Dump processor state."
  ([processor] (dump-processor processor processor))
  ([previous processor]
   (print (format "D: %02x  DF:%x  P: %x  X: %x\n"
                  (:D processor)
                  (:DF processor)
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
         (print (format "mem %04x: %02x\n" addr (:value val))))))))

(defn format-address
  "Format memory address and its contents."
  [addr val]
  (str (format "%04x: " addr) (format-instruction val)))

(defn lst
  "List memory contents."
  ([start end execution]
   (let [memory (:mem (first execution))]
     (map (fn [addr] (format-address addr (get-in memory [addr])))
          (range start (+ end 1))))))

;;;
;;; Debug utilities.
;;;

(defn debug-lisp [input]
  (execution (prog) (reader (str input "\r\004"))))

(defn break-at [addr execution]
  (drop-while (fn [processor]
                (not= (get-in processor [:R (:P processor)])
                      addr))
              execution))

(defn trace [count execution]
  (doseq [processor (take count execution)]
    (dump-instruction
     (get-in processor
             [:mem (get-in processor [:R (:P processor)])]))
    (dump-processor processor (next-state processor))))
