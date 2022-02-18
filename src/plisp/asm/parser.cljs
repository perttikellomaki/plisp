(ns plisp.asm.parser
  (:require
   [cljs.reader :as reader]
   [clojure.string :as str]))

;;;
;;; Parsers for lines of assembly.
;;;

(defn no-operand-op
  "Parse a single byte operation with no register part."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s*(;.*)*" "OP" op))
        [_ op] (re-matches re line)]
    (when op
      {:op (keyword op) :bytes 1})))

(defn register-op
  "Parse a register operation."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg] (re-matches re line)]
    (when op
      {:op (keyword op) :n (reader/read-string (str "0x" reg)) :bytes 1})))

(defn immediate-op
  "Parse an immediate operation."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+#([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op operand] (re-matches re line)]
    (when op
      {:op (keyword op) :immediate (reader/read-string (str "0x" operand)) :bytes 2})))

(defn register-immediate-op
  "Parse a register immediate operation."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+#([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg operand] (re-matches re line)]
    (when op
      {:op (keyword op) :n (reader/read-string (str "0x" reg)) :long-immediate (reader/read-string (str "0x" operand)) :bytes 4})))

(defn subroutine-call-op
  "Parse a subroutine call instruction."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s+([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg address] (re-matches re line)]
    (when op
      {:op (keyword op) :n (reader/read-string (str "0x" reg)) :long-address (reader/read-string (str "0x" address)) :bytes 4})))

(defn extended-register-op
  "Parse a subroutine call instruction."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op reg] (re-matches re line)]
    (when op
      {:op (keyword op) :n (reader/read-string (str "0x" reg)) :bytes 2})))

(defn short-branch-op
  "Parse a short branch operation."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op address] (re-matches re line)]
    (when op
      {:op (keyword op) :page-address (reader/read-string (str "0x" address)) :bytes 2})))

(defn long-branch-op
  "Parse a long branch operation."
  [op line]
  (let [re (re-pattern (str/replace "\\s*(OP)\\s+([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F])\\s*(;.*)*" "OP" op))
        [_ op address] (re-matches re line)]
    (when op
      {:op (keyword op) :long-address (reader/read-string (str "0x" address)) :bytes 3})))

(defn address-directive
  "Parse an address directive."
  [line]
  (let [[_ address] (re-matches #"\s*([0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]):\s*(;.*)*" line)]
    (when address
      {:op :address :address (reader/read-string (str "0x" address))})))

(defn byte-directive
  "Parse a byte directive."
  [line]
  (let [[_ byte] (re-matches #"\s*BYTE\s+#([0-9a-fA-F][0-9a-fA-F])\s*(;.*)*" line)]
    (when byte
      {:op :byte :value (reader/read-string (str "0x" byte)) :bytes 1})))

(defn string-directive
  "Parse a string directive."
  [line]
  (let [[_ str] (re-matches #"\s*STRING\s+\"([^\"]*)\"\s*(;.*)*" line)]
    (when str
      {:op :string :value str :bytes (count str)})))

(defn empty-line
  "Parse a comment line."
  [line]
  (when (re-matches #"\s*(;.*)*" line)
    {:op :empty :bytes 0}))

(defn- parse-instruction-line [line]
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
    (short-branch-op "BDF" line)
   ;; 0x34
   ;; 0x35
   ;; 0x36
   ;; 0x37
   ;; 0x38
    (no-operand-op "SKP" line)
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
    (no-operand-op "LSKP" line)
   ;; 0xC9
   ;; 0xCA
    (long-branch-op "LBNZ" line)
   ;; 0xCB
   ;; 0xCC
   ;; 0xCD
   ;; 0xCE
    (no-operand-op "LSZ" line)
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
    (no-operand-op "SD" line)
   ;; 0xF6
    (no-operand-op "SHR" line)
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
    (no-operand-op "READCHAR" line)))

(defn parse-line
  "Parse a single line of assembly."
  [index line]
  (-> (parse-instruction-line line)

      ;; Attach debug info
      (assoc :source-line line)
      (assoc :source-line-number index)))

