(ns plisp.cosmac.processor
  (:require [plisp.util :refer [reg16 int16]]))

(defn- byte-in-memory [address memory]
  (let [{:keys [op value] :as memory-contents} (get memory address)]
    (cond (nil? memory-contents) 0x00
          (= op :byte)           value
          :else                  nil)))

(defn word-in-memory [address memory]
  (when-let [hi-byte (byte-in-memory address memory)]
    (when-let [low-byte (byte-in-memory (inc address) memory)]
      (+ (* 0x100 hi-byte) low-byte))))

;;;
;;; The processor state.
;;;
;;; To simplify things, textual input and output are modeled as part
;;; of processor state. Input for a program is read from the sequnce
;;; input-buffer, and output produced by the program is stored in
;;; output-buffer. Initial input can be given at reset, which is
;;; convenient for running tests.
;;;

(def zeroed-registers
  "Register bank of sixteen 16-bit registers"
  (reduce (fn [regs n] (assoc regs n {:hi 0x00 :lo 0x00}))
          {}
          (range 16)))

(defn reset
  "Initial state of the processor. Optionally with starting address in R0."
  ([prog] (reset prog 0x0000))
  ([prog start-addr] (reset prog start-addr []))
  ([prog start-addr input-buffer]
   {:D 0x00
    :DF 0
    :P 0x0
    :X 0x0
    :R (-> zeroed-registers
           (assoc 0 (reg16 start-addr)))
    :mem prog
    :input-buffer input-buffer
    :output-buffer []
    :status :running
    :instruction-count 0
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
  (let [pc (int16 (get-in processor [:R (:P processor)]))
        instruction (get-in processor [:mem pc])]
    [instruction
     (when instruction
       (assoc-in processor
                 [:R (:P processor)]
                 (reg16 (+ pc (:bytes instruction)))))]))

;;;
;;; Helpers for implementing instuction semantics.
;;;

(defn mem-byte [val] {:op :byte :value val})

(defn inc16 [reg]
  (-> (int16 reg)
      inc
      reg16))

(defn dec16 [reg]
  (-> (int16 reg)
      dec
      reg16))

(defn short-branch [page-address condition pc]
  (if condition
    page-address
    (:lo pc)))

(defn long-branch [long-address condition pc]
  (if condition
    (reg16 long-address)
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

(defn- instruction-effect
  [{:keys [op n immediate long-immediate page-address long-address value] :as instruction}
   {:keys [input-buffer output-buffer] :as _processor}
   {:keys [P X D DF mem R]}]

  (let [the identity] ; syntactic sugar
    (case op
      :IDLE [[:status]                      #(the :idle)]
      :LDN  [[:D]                           #(mem (R n))]
      :INC  [[:R n]                         #(inc16 (R n))]
      :DEC  [[:R n]                         #(dec16 (R n))]
      :BR   [[:R (P) :lo]                   #(short-branch page-address true (R (P)))]
      :BZ   [[:R (P) :lo]                   #(short-branch page-address (= (D) 0) (R (P)))]
      :BDF  [[:R (P) :lo]                   #(short-branch page-address (= (DF) 1) (R (P)))]
      :SKP  [[:R (P)]                       #(inc16 (R (P)))]
      :BNZ  [[:R (P) :lo]                   #(short-branch page-address (not= (D) 0) (R (P)))]
      :LDA  [[:D]                           #(mem (R n))
             [:R n]                         #(inc16 (R n))]
      :STR  [[:mem (int16 (R n))]           #(mem-byte (D))]
      :RLXA [[:R (X)]                       #(inc16 (inc16 (R (X))))
             [:R n :hi]                     #(mem (R (X)))
             [:R n :lo]                     #(mem (inc16 (R (X))))]
      :SCAL [[:mem (int16 (R (X)))]         #(mem-byte (:lo (R n)))
             [:mem (int16 (dec16 (R (X))))] #(mem-byte (:hi (R n)))
             [:R (X)]                       #(dec16 (dec16 (R (X))))
             [:R n]                         #(R (P))
             [:R (P)]                       #(reg16 long-address)]
      :SRET [[:R (P)]                       #(R n)
             [:R n :hi]                     #(mem (inc16 (R (X))))
             [:R n :lo]                     #(mem (inc16 (inc16 (R (X)))))
             [:R (X)]                       #(inc16 (inc16 (R (X))))]
      :RSXD [[:mem (int16 (R (X)))]         #(mem-byte (:lo (R n)))
             [:mem (int16 (dec16 (R (X))))] #(mem-byte (:hi (R n)))
             [:R (X)]                       #(dec16 (dec16 (R (X))))]
      :RNX  [[:R (X)]                       #(R n)]
      :RLDI [[:R n]                         #(reg16 long-immediate)]
      :STXD [[:mem (int16 (R (X)))]         #(mem-byte (D))
             [:R (X)]                       #(dec16 (R (X)))]
      :ADCI [[:D]                           #(bit-and 0xff (+ (D) (DF) immediate))
             [:DF]                          #(if (> (+ (D) (DF) immediate) 0xff) 1 0)]
      :SMBI [[:D]                           #(bit-and
                                              0xff
                                              (- (D)
                                                 immediate
                                                 (if (= (DF) 0) 1 0)))
             [:DF]                          #(if (>= (- (D) immediate
                                                        (if (= (DF) 0) 1 0))
                                                     0)
                                               1
                                               0)]
      :GLO  [[:D]                           #(:lo (R n))]
      :GHI  [[:D]                           #(:hi (R n))]
      :PLO  [[:R n :lo]                     #(D)]
      :PHI  [[:R n :hi]                     #(D)]
      :LBR  [[:R (P)]                       #(reg16 long-address)]
      :NOP  []
      :LSNZ [[:R (P)]                       #(if (not= (D) 0)
                                               (inc16 (inc16 (R (P))))
                                               (R (P)))]
      :LSKP [[:R (P)]                       #(inc16 (inc16 (R (P))))]
      :LBNZ [[:R (P)]                       #(long-branch long-address
                                                          (not= (D) 0)
                                                          (R (P)))]
      :LSZ  [[:R (P)]                       #(if (= (D) 0)
                                               (inc16 (inc16 (R (P))))
                                               (R (P)))]
      :SEP  [[:P]                           #(the n)]
      :SEX  [[:X]                           #(the n)]
      :OR   [[:D]                           #(bit-or (D) (mem (R (X))))]
      :XOR  [[:D]                           #(bit-xor (D) (mem (R (X))))]
      :ADD  [[:D]                           #(bit-and 0xff (+ (D) (mem (R (X)))))
             [:DF]                          #(if (> (+ (D) (mem (R (X)))) 0xff) 1 0)]
      :SD   [[:D]                           #(bit-and 0xff (- (mem (R (X))) (D)))
             [:DF]                          #(if (>= (mem (R (X))) (D)) 1 0)]
      :SHR  [[:D]                           #(bit-shift-right (D) 1)
             [:DF]                          #(bit-and 0x01 (D))]
      :LDI  [[:D]                           #(the immediate)]
      :ORI  [[:D]                           #(bit-or (D) immediate)]
      :ANI  [[:D]                           #(bit-and (D) immediate)]
      :XRI  [[:D]                           #(bit-xor (D) immediate)]
      :ADI  [[:D]                           #(bit-and 0xff (+ (D) immediate))
             [:DF]                          #(if (> (+ (D) immediate) 0xff) 1 0)]
      :SMI  [[:D]                           #(bit-and 0xff (- (D) immediate))
             [:DF]                          #(if (>= (D) immediate) 1 0)]
      :PRINTCHAR
            [[:output-buffer]               #(conj output-buffer (char (D)))]
      :READCHAR
            [[:D]                           #(if (empty? input-buffer) (D) (.charCodeAt (first input-buffer)))
             [:status]                      #(if (empty? input-buffer) :read-blocked :running)
             [:R (P)]                       #(if (empty? input-buffer)
                                               ;; back up R(P) to point to the READCHAR instruction
                                               (reg16 (- (int16 (R (P))) (:bytes instruction)))
                                               (R (P)))
             [:input-buffer]                #(rest input-buffer)]

    ;; Just enough support for executing hex coded instructions
    ;; to get the Lisp running.
      :byte
      (if (= value 0xc0)
        [[:R (P) :hi]                   #(mem (R (P)))
         [:R (P) :lo]                   #(mem (inc16 (R (P))))]

          ;; Silent NOP for unknown hex instructions
        []))))

(defn next-state
  "Return the next state of the processor after executing one instruction."
  [initial-state]
  (if (or (= (:status initial-state) :idle)
          (and (= (:status initial-state) :read-blocked)
               (empty? (:input-buffer initial-state))))

    ;; No instruction fetch if waiting for input
    initial-state

    (let [[instruction processor] (instruction-fetch initial-state)]
      (when instruction
        (let [effect (instruction-effect instruction
                                         processor
                                           ;; Convenience accessors
                                         {:P   #(:P processor)
                                          :X   #(:X processor)
                                          :D   #(:D processor)
                                          :DF  #(:DF processor)
                                          :mem #(or (:value (get-in processor [:mem (int16 %)]))
                                                      ;; assume uninitialized memory is zeroed out
                                                    0x00)
                                          :R   #(get-in processor [:R %])})
              final-state (when effect
                            (execute-instruction processor effect))]
          (update-in final-state [:instruction-count] inc))))))
