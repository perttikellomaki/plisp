(ns plisp.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [clojure.data :as data]
            [clojure.string :as str]
            [plisp.asm.memory :as memory]
            [plisp.asm.parser :as parser]
            [plisp.asm.lisp :as lisp :refer [run-lisp]]
            [plisp.cosmac.processor :as processor]
            ))

;;;
;;; Tests for individual instructions
;;;

(defn initial-processor [instructions input]
  (processor/reset
   (:memory (memory/layout
             (map-indexed parser/parse-line instructions)))
   0x0000
   input))

(defn run-prog
  [instructions & [{:keys [n input]
                    :or {n     (count instructions)
                         input []}}]]
   (let [initial (initial-processor instructions input)]
     (loop [processor initial
            n n]
       (if (= n 0)
         (let [[_ changes _] (data/diff initial processor)]
           changes)
         (recur (processor/next-state processor) (- n 1))))))

(deftest test-idle
  (let [changes
        (run-prog
         ["  IDLE"])]
    (is (= {:R {0 {:lo 1}}
            :instruction-count 1
            :status :idle}
           changes))))

(deftest test-ldn
  (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDN 1"
          "fffe::"
          "  BYTE #12"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x05}
                1 {:hi 0xff :lo 0xfe}}
            :instruction-count 2}
           changes))))

(deftest test-inc
  (let [changes
        (run-prog
         ["  INC 1"])]
    (is (= changes
           {:R {0 {:lo 0x01}
                1 {:lo 0x01}}
            :instruction-count 1})))
  (let [changes
        (run-prog
         ["  RLDI 5 #ffff"
          "  INC 5"
          "  INC 5"])]
    (is (= {:R {0 {:lo 6}
                5 {:lo 0x01}}
            :instruction-count 3}
           changes))))

(deftest test-dec
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  DEC 1"])]
    (is (= changes
           {:R {0 {:lo 0x05}
                1 {:hi 0x12 :lo 0x33}}
            :instruction-count 2})))
  (let [changes
        (run-prog
         ["  DEC 5"
          "  DEC 5"])]
    (is (= {:R {0 {:lo 0x02}
                5 {:hi 0xff :lo 0xfe}}
            :instruction-count 2}
           changes))))

(deftest test-br
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BR 06"
          "  LDI #34"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x06}}
            :instruction-count 2}
           changes))))

(deftest test-bz
  (let [changes
        (run-prog
         ["  LDI #00"
          "  BZ 08"
          "  LDI #34"]
         {:n 2})]
    (is (= {:R {0 {:lo 0x08}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BZ 06"
          "  LDI #34"])]
    (is (= {:D 0x34
            :R {0 {:lo 0x06}}
            :instruction-count 3}
           changes))))

(deftest test-bnf
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #01"
          "  BNF 10"]
         {:n 3})]
    (is (= {:R {0 {:lo 0x06}}
            :DF 1
            :instruction-count 3}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #02"
          "  BNF 10"]
         {:n 3})]
    (is (= {:D 0xff
            :R {0 {:lo 0x10}}
            :instruction-count 3}
           changes))))

(deftest test-bdf
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #01"
          "  BDF 10"]
         {:n 3})]
    (is (= {:R {0 {:lo 0x10}}
            :DF 1
            :instruction-count 3}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #02"
          "  BDF 10"]
         {:n 3})]
    (is (= {:D 0xff
            :R {0 {:lo 0x06}}
            :instruction-count 3}
           changes))))

(deftest test-skp
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  SKP"
          "  INC 1"
          "  NOP"]
         {:n 3})]
    (is (= {:R {0 {:lo 0x07}
                1 {:hi 0x12 :lo 0x34}}
            :instruction-count 3}
           changes))))

(deftest test-bnz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BNZ 06"
          "  LDI #34"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x06}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  BNZ 06"
          "  LDI #34"]
         {:n 3})]
    (is (= {:D 0x34
            :R {0 {:lo 0x06}}
            :instruction-count 3}
           changes))))

(deftest test-lda
  (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDA 1"
          "fffe::"
          "  BYTE #12"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x05}
                1 {:hi 0xff :lo 0xff}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDA 1"
          "  LDA 1"
          "fffe::"
          "  BYTE #12"
          "  BYTE #34"]
         {:n 3})]
    (is (= {:D 0x34
            :R {0 {:lo 0x06}} ; R1 wraps over to zero
            :instruction-count 3}
           changes))))

(deftest test-str
  (let [changes
        (run-prog
         ["  RLDI 1 #ffff"
          "  LDI #12"
          "  STR 1"])]
    (is (= {:D 0x12
            :R {0 {:lo 0x07}
                1 {:hi 0xff :lo 0xff}}
            :instruction-count 3
            :mem {0xffff (processor/mem-byte 0x12)}}
           changes))))

(deftest test-rlxa
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  RLXA 2"
          "0100::"
          "  BYTE #12"
          "  BYTE #34"]
         {:n 3})]
    (is (= {:R {0 {:lo 0x07}
                1 {:hi 0x01 :lo 0x02}
                2 {:hi 0x12 :lo 0x34}}
            :X 1
            :instruction-count 3}
           changes))))

(deftest test-scal-sret
  (let [changes
        (run-prog
         ["  RLDI 2 #ffff"
          "  SEX 2"
          "  SCAL 1 0100"
          "  BYTE #03"
          "  RLDI 5 #0550"
          "0100::"
          "  LDA 1   ; should pick up the #03 at call site"
          "  PHI 3"
          "  SCAL 1 0200"
          "  LDI #30"
          "  PLO 3"
          "  SRET 1"
          "0200::"
          "  RLDI 4 #0440"
          "  SRET 1"]
         {:n 12})]
    (is (= {:R {0 {:lo 14}
                2 {:hi 0xff :lo 0xff}
                3 {:hi 0x03 :lo 0x30}
                4 {:hi 0x04 :lo 0x40}
                5 {:hi 0x05 :lo 0x50}}
            :D 0x30
            :X 2
            :mem {0xfffc (processor/mem-byte 0x00)
                  0xfffd (processor/mem-byte 0x0a)
                  0xfffe (processor/mem-byte 0x00)
                  0xffff (processor/mem-byte 0x00)}
            :instruction-count 12}
           changes))))

(deftest test-rsxd
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  RLDI 2 #ffff"
          "  SEX 2"
          "  RSXD 1"])]
    (is (= {:R {0 {:lo 0x0b}
                1 {:hi 0x12 :lo 0x34}
                2 {:hi 0xff :lo 0xfd}}
            :X 2
            :mem {0xfffe (processor/mem-byte 0x12)
                  0xffff (processor/mem-byte 0x34)}
            :instruction-count 4}
           changes))))

(deftest test-rnx
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  SEX 2"
          "  RNX 1"])]
    (is (= {:R {0 {:lo 0x07}
                1 {:hi 0x12 :lo 0x34}
                2 {:hi 0x12 :lo 0x34}}
            :X 2
            :instruction-count 3}
           changes))))

(deftest test-rldi
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"])]
    (is (= {:R {0 {:lo 0x04}
                1 {:hi 0x12 :lo 0x34}}
            :instruction-count 1}
           changes))))

(deftest test-stxd
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #34"
          "  STXD"])]
    (is (= {:D 0x34
            :X 1
            :R {0 {:lo 0x08}
                1 {:lo 0xff}}
            :mem {0x0100 (processor/mem-byte 0x34)}
            :instruction-count 4}
           changes))))

(deftest test-adc
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  RLDI 2 #0102"
          "  RLDI 3 #0104"
          "  SEX 2"
          "  LDA 1"
          "  ADD"
          "  STR 3"
          "  INC 3"
          "  INC 2"
          "  LDA 1"
          "  ADC"
          "  STR 3"
          "0100::"
          "  BYTE #10"
          "  BYTE #20"
          "  BYTE #30"
          "  BYTE #40"]
         {:n 12})]
    (is (= {:D 0x60
            :X 2
            :R {0 {:lo 0x15}
                1 {:hi 0x01 :lo 0x02}
                2 {:hi 0x01 :lo 0x03}
                3 {:hi 0x01 :lo 0x05}
                }
            :instruction-count 12
            :mem {0x0104 {:op :byte, :value 0x40}
                  0x0105 {:op :byte, :value 0x60}}}
           changes)))
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  RLDI 2 #0102"
          "  RLDI 3 #0104"
          "  SEX 2"
          "  LDA 1"
          "  ADD"
          "  STR 3"
          "  INC 3"
          "  INC 2"
          "  LDA 1"
          "  ADC"
          "  STR 3"
          "0100::"
          "  BYTE #91"
          "  BYTE #20"
          "  BYTE #82"
          "  BYTE #40"]
         {:n 12})]
    (is (= {:D 0x61
            :X 2
            :R {0 {:lo 0x15}
                1 {:hi 0x01 :lo 0x02}
                2 {:hi 0x01 :lo 0x03}
                3 {:hi 0x01 :lo 0x05}
                }
            :instruction-count 12
            :mem {0x0104 {:op :byte, :value 0x13}
                  0x0105 {:op :byte, :value 0x61}}}
           changes))))

(deftest test-adci
  (let [changes
        (run-prog
         ;; calculate 0x0001 + 0x0002 to R1
         ["  LDI #01"
          "  ADI #02"
          "  PLO 1"
          "  LDI #00"
          "  ADCI #00"
          "  PHI 1"])]
    (is (= {:R {0 {:lo 0x0a}
                1 {:lo 0x03}}
            :instruction-count 6}
           changes)))
  (let [changes
        (run-prog
         ;; calculate 0x00ff + 0x0002 to R1
         ["  LDI #ff"
          "  ADI #02"
          "  PLO 1"
          "  LDI #00"
          "  ADCI #00"
          "  PHI 1"])]
    (is (= {:D 0x01
            :R {0 {:lo 0x0a}
                1 {:hi 0x01 :lo 0x01}}
            :instruction-count 6}
           changes))))

(deftest test-smbi
  (let [changes
        (run-prog
         ;; calculate 0x0000 - 0x0001 to R1
         ["  LDI #00"
          "  SMI #01"
          "  PLO 1"
          "  LDI #00"
          "  SMBI #00"
          "  PHI 1"])]
    (is (= {:D 0xff
            :R {0 {:lo 0x0a}
                1 {:hi 0xff :lo 0xff}}
            :instruction-count 6}
           changes)))
  (let [changes
        (run-prog
         ;; calculate 0x5678 - 0x1234 to R1
         ["  LDI #78"
          "  SMI #34"
          "  PLO 1"
          "  LDI #56"
          "  SMBI #12"
          "  PHI 1"])]
    (is (= {:D 0x44
            :DF 1
            :R {0 {:lo 0x0a}
                1 {:hi 0x44 :lo 0x44}}
            :instruction-count 6}
           changes)))
  (let [changes
        (run-prog
         ;; calculate 0x1234 - 0x5678 to R1
         ["  LDI #34"
          "  SMI #78"
          "  PLO 1"
          "  LDI #12"
          "  SMBI #56"
          "  PHI 1"])]
    (is (= {:D 0xbb
            :R {0 {:lo 0x0a}
                1 {:hi 0xbb :lo 0xbc}}
            :instruction-count 6}
           changes))))

(deftest test-plo-glo
  (let [changes
        (run-prog
         ["  RLDI 1 #ffff"
          "  LDI #12"
          "  PLO 1"
          "  LDI #34"
          "  GLO 1"])]
    (is (= {:D 0x12
            :R {0 {:lo 0x0a}
                1 {:hi 0xff :lo 0x12}}
            :instruction-count 5}
           changes))))

(deftest test-phi-ghi
  (let [changes
        (run-prog
         ["  RLDI 1 #ffff"
          "  LDI #12"
          "  PHI 1"
          "  LDI #34"
          "  GHI 1"])]
    (is (= {:D 0x12
            :R {0 {:lo 0x0a}
                1 {:hi 0x12 :lo 0xff}}
            :instruction-count 5}
           changes))))

(deftest test-lbr
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LBR 2000"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:hi 0x20}}
            :instruction-count 2}
           changes))))

(deftest test-lbz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LBZ 2000"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x05}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  LBZ 2000"]
         {:n 2})]
    (is (= {:R {0 {:hi 0x20}}
            :instruction-count 2}
           changes))))

(deftest test-nop
  (let [changes
        (run-prog
         ["  NOP"])]
    (is (= {:R {0 {:lo 0x01}}
            :instruction-count 1}
           changes))))

(deftest test-lsnz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LSNZ"
          "  LDI #34"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x05}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  LSNZ"
          "  LDI #34"])]
    (is (= {:D 0x34
            :R {0 {:lo 0x05}}
            :instruction-count 3}
           changes))))

(deftest test-lskp
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LSKP"
          "  LDI #34"]
         {:n 2})]
    (is (= {:D 0x12
            :R {0 {:lo 0x05}}
            :instruction-count 2}
           changes))))

(deftest test-lbnz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LBNZ 0100"
          "  LDI #34"
          "0100::"
          "  NOP"]
         {:n 3})]
    (is (= {:D 0x12
            :R {0 {:hi 0x01 :lo 0x01}}
            :instruction-count 3}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  LBNZ 0100"
          "  LDI #34"
          "0100::"
          "  NOP"]
         {:n 3})]
    (is (= {:D 0x34
            :R {0 {:lo 0x07}}
            :instruction-count 3}
           changes))))

(deftest test-lsz
  (let [changes
        (run-prog
         ["  LDI #00"
          "  LSZ"
          "  LDI #34"]
         {:n 2})]
    (is (= {:R {0 {:lo 0x05}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LSZ"
          "  LDI #34"])]
    (is (= {:D 0x34
            :R {0 {:lo 0x05}}
            :instruction-count 3}
           changes))))

(deftest test-sep
  (let [changes
        (run-prog
         ["  LDI #12"
          "  RLDI 1 #0009"
          "  SEP 1"]
         {:n 3})]
    (is (= {:D 0x12
            :P 1
            :R {0 {:lo 0x07}
                1 {:lo 0x09}}
            :instruction-count 3}
           changes))))

(deftest test-sex
  (let [changes
        (run-prog
         ["  SEX 1"])]
    (is (= {:X 1
            :R {0 {:lo 0x01}}
            :instruction-count 1}
           changes))))

(deftest test-or
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #0f"
          "  STR 1"
          "  LDI #f0"
          "  OR"])]
    (is (= {:D 0xff
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0x0f}}}
           changes))))

(deftest test-xor
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #0f"
          "  STR 1"
          "  LDI #ff"
          "  XOR"])]
    (is (= {:D 0xf0
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0x0f}}}
           changes))))

(deftest test-add
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #01"
          "  STR 1"
          "  LDI #02"
          "  ADD"])]
    (is (= {:D 0x03
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0x01}}}
           changes)))
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #ff"
          "  STR 1"
          "  LDI #02"
          "  ADD"])]
    (is (= {:D 0x01
            :DF 1
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0xff}}}
           changes))))

(deftest test-sd
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #01"
          "  STR 1"
          "  LDI #02"
          "  SD"])]
    (is (= {:D 0xff
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0x01}}}
           changes)))
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  LDI #ff"
          "  STR 1"
          "  LDI #01"
          "  SD"])]
    (is (= {:D 0xfe
            :DF 1
            :X 1
            :R {0 {:lo 0x0b}
                1 {:hi 0x01}}
            :instruction-count 6
            :mem {0x0100 {:op :byte, :value 0xff}}}
           changes))))

(deftest test-shr
  (let [changes
        (run-prog
         ["  LDI #AA"
          "  SHR"])]
    (is (= {:D 0x55
            :R {0 {:lo 0x03}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #55"
          "  SHR"])]
    (is (= {:D 0x2a
            :DF 1
            :R {0 {:lo 0x03}}
            :instruction-count 2}
           changes))))

(deftest test-ldi
  (let [changes
        (run-prog
         ["  LDI #12"])]
    (is (= {:D 0x12
            :R {0 {:lo 0x02}}
            :instruction-count 1}
           changes))))

(deftest test-ori
  (let [changes
        (run-prog
         ["  LDI #0f"
          "  ORI #f0"])]
    (is (= {:D 0xff
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes))))

(deftest test-ani
  (let [changes
        (run-prog
         ["  LDI #12"
          "  ANI #0f"])]
    (is (= {:D 0x02
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes))))

(deftest test-xri
  (let [changes
        (run-prog
         ["  LDI #12"
          "  XRI #34"])]
    (is (= {:D 0x26
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes))))

(deftest test-adi
  (let [changes
        (run-prog
         ["  LDI #34"
          "  ADI #12"])]
    (is (= {:D 0x46
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #fe"
          "  ADI #03"])]
    (is (= {:D 0x01
            :DF 1
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes))))

(deftest test-smi
  (let [changes
        (run-prog
         ["  LDI #34"
          "  SMI #12"])]
    (is (= {:D 0x22
            :DF 1
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #02"])]
    (is (= {:D 0xff
            :R {0 {:lo 0x04}}
            :instruction-count 2}
           changes))))

(deftest test-byte
  (let [changes
        (run-prog
         ["  BYTE #C0"
          "  BYTE #12"
          "  BYTE #34"]
         {:n 1})]
    (is (= {:R {0 {:hi 0x12 :lo 0x34}}
            :instruction-count 1}
           changes))))

(deftest test-printchar []
  (testing "Print single character"
    (let [changes
          (run-prog
           ["  LDI #31"
            "  PRINTCHAR"])]
      (is (= {:output-buffer ["1"]}
             (dissoc changes :D :R :instruction-count)))))
  (testing "Print multiple characters"
    (let [changes
          (run-prog
           ["  LDI #31"
            "  PRINTCHAR"
            "  PRINTCHAR"
            "  LDI #41"
            "  PRINTCHAR"
            "  LDI #42"
            "  PRINTCHAR"])]
      (is (= {:output-buffer ["1" "1" "A" "B"]}
             (dissoc changes :D :R :instruction-count))))))

(deftest test-readchar []
  (testing "Read single character"
    (let [changes
          (run-prog
           ["  READCHAR"]
           {:input ["0" "1"]})]
      (is (= {:D (.charCodeAt "0")
              :R {0 {:lo 0x01}}
              :instruction-count 1
              :input-buffer ["1"]}
             changes))))
  (testing "Read multiple characters"
    (let [changes
          (run-prog
           ["  READCHAR"
            "  READCHAR"
            "  READCHAR"]
           {:input ["0" "1" "2" "3"]})]
      (is (= {:D (.charCodeAt "2")
              :R {0 {:lo 0x03}}
              :instruction-count 3
              :input-buffer ["3"]}
             changes))))
  (testing "Read blocking"
    (let [changes
          (run-prog
           ["  READCHAR"
            "  READCHAR"]
           {:input ["0"]})]
      (is (= {:D (.charCodeAt "0")
              :R {0 {:lo 0x01}}
              :input-buffer nil
              :instruction-count 2
              :status :read-blocked}
             changes))
      (is (empty? (:input-buffer changes))))))
            
;;;
;;; Lisp tests
;;;

(defn lisp-output [& outputs]
  (str " P-LISP FOR 1805 vers 1.0 210884\r\n C 1984 PERTTI KELLOMÄKI      \r\n CELLS FREE\r\nLISP RUNNING\r\n\r\n-"
       (str/join "\r\n-" outputs)
       "\r\n-"))
       
(deftest test-lisp-nil
  (is (= (run-lisp "()")
         (lisp-output "NIL"))))

(deftest test-lisp-atom-nil
  (is (= (run-lisp "NIL")
         (lisp-output "NIL"))))

(deftest test-lisp-atom-t
  (is (= (run-lisp "T")
         (lisp-output "T"))))

(deftest test-lisp-nil-t-nil
  (is (= (run-lisp "()\rT\r()")
         (lisp-output "NIL" "T" "NIL"))))

(deftest test-lisp-cons
  (is (= (run-lisp "(CONS T NIL)")
         (lisp-output "(T)")))
  (is (= (run-lisp "(CONS T (CONS NIL NIL))")
         (lisp-output "(T NIL)")))
  (is (= (run-lisp "(CONS (CONS T NIL) (CONS NIL NIL))")
         (lisp-output "((T) NIL)")))
  (is (= (run-lisp "(CONS (CONS T T) (CONS NIL NIL))")
         (lisp-output "((T . T) NIL)"))))

(deftest test-quote
  (is (= (run-lisp "(QUOTE FOO)")
         (lisp-output "FOO")))
  (is (= (run-lisp "(CONS (QUOTE FOO) NIL)")
         (lisp-output "(FOO)")))
  (is (= (run-lisp "(QUOTE ((FOO BAR) BAZ))")
         (lisp-output "((FOO BAR) BAZ)"))))

(deftest test-car
  (is (= (run-lisp "(CAR (CONS T NIL))")
         (lisp-output "T")))
  (is (= (run-lisp "(CAR (QUOTE (FOO BAR)))")
         (lisp-output "FOO")))
  (is (= (run-lisp "(CAR (QUOTE ((FOO) BAR)))")
         (lisp-output "(FOO)"))))

(deftest test-cdr
  (is (= (run-lisp "(CDR (CONS NIL T))")
         (lisp-output "T")))
  (is (= (run-lisp "(CDR (QUOTE (FOO BAR)))")
         (lisp-output "(BAR)")))
   (is (= (run-lisp "(CDR (QUOTE (FOO (BAR))))")
         (lisp-output "((BAR))"))))
