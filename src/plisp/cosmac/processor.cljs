(ns plisp.cosmac.processor)

;;;
;;; The processor state.
;;;
;;; To simplify things, textual input and output are modeled as part
;;; of processor state. Input for a program is read from the sequnce
;;; input-buffer, and output produced by the program is stored in
;;; output-buffer. Initial input can be given at reset, which is
;;; convenient for running tests.
;;;

(defn reset
  "Initial state of the processor. Optionally with starting address in R0."
  ([prog] (reset prog 0x0000))
  ([prog start-addr] (reset prog start-addr []))
  ([prog start-addr input-buffer]
   {:D 0x00
    :DF 0
    :P 0x0
    :X 0x0
    :R [start-addr 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000 0X0000]
    :mem prog
    :input-buffer input-buffer
    :output-buffer []
    :status :running
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

(defn long-branch [long-address condition pc]
  (if condition
    long-address
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
   (let [instruction-addr (get-in initial-processor [:R (:P initial-processor)])
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
         (let [{:keys [n immediate long-immediate page-address long-address]} instruction
               effect (case (:op instruction)
                          :IDLE [[:status]
                                 (fn [] :idle)]
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
                          :BDF [[:R (P)]
                                (fn [] (short-branch page-address (= (DF) 1) (R (P))))]
                          :SKP [[:R (P)]
                                (fn [] (inc-16bit (R (P))))]
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
                          :LSKP [[:R (P)]
                                 (fn [] (inc-16bit (inc-16bit (R (P)))))]
                          :LBNZ [[:R (P)]
                                 (fn [] (long-branch long-address
                                                     (not= (D) 0)
                                                     (R (P))))]
                          :LSZ [[:R (P)]
                                (fn [] (if (= (D) 0)
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
                          :SD [[:D]
                               (fn [] (bit-and 0xff (- (mem (R (X))) (D))))
                               [:DF]
                               (fn [] (if (>= (mem (R (X))) (D)) 1 0))]
                          :SHR [[:D]
                                (fn [] (bit-shift-right (D) 1))
                                [:DF]
                                (fn [] (bit-and 0x01 (D)))]
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
                          :PRINTCHAR [[:output-buffer]
                                      (fn [] (conj (:output-buffer processor) (char (D))))]
                          :READCHAR (if (empty? (:input-buffer processor))
                                      ;; Undo instruction fetch, i.e. block
                                      [[:R (P) ]
                                       (fn [] instruction-addr)
                                       [:status]
                                       (fn [] :read-blocked)]
                                      [[:D]
                                       (fn [] (.charCodeAt (first (:input-buffer processor))))
                                       [:input-buffer]
                                       (fn []
                                         (rest (:input-buffer processor)))])

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
                             (execute-instruction processor effect))]
           final-state))))))
