(ns plisp.core-test
  (:require [clojure.test :refer :all]
            [plisp.core :refer :all]))

(use 'clojure.data)

(defn initial-processor [instructions]
  (reset (layout (map parse-line instructions))))

(defn run-prog
  ([instructions] (run-prog instructions (count instructions)))
  ([instructions n]
   (let [initial (initial-processor instructions)]
     (loop [processor initial
            n n]
       (if (= n 0)
         (let [[_ changes _] (diff initial processor)]
           changes)
         (recur (next-state processor) (- n 1)))))))

(deftest test-idle
  (let [changes
        (run-prog
         ["  IDLE"])]
    (is (= {:R [1]
            :running false}
           changes))))

(deftest test-ldn
  (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDN 1"
          "fffe:"
          "  BYTE #12"]
         2)]
    (is (= {:D 0x12
            :R [5 0xfffe]}
           changes))))

(deftest test-inc
  (let [changes
        (run-prog
         ["  INC 1"])]
    (is (= changes
           {:R [1 1]})))
  (let [changes
        (run-prog
         ["  RLDI 5 #ffff"
          "  INC 5"
          "  INC 5"])]
    (is (= {:R [6 nil nil nil nil 0x0001]}
           changes))))

(deftest test-dec
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  DEC 1"])]
    (is (= changes
           {:R [5 0X1233]})))
  (let [changes
        (run-prog
         ["  DEC 5"
          "  DEC 5"])]
    (is (= {:R [2 nil nil nil nil 0xfffe]}
           changes))))

(deftest test-br
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BR 06"
          "  LDI #34"]
         2)]
    (is (= {:D 0x12
            :R [0x0006]}
           changes))))

(deftest test-bz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LDI #00"
          "  BZ 08"
          "  LDI #34"]
         3)]
    (is (= {:R [0x0008]}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BZ 06"
          "  LDI #34"])]
    (is (= {:D 0x34
            :R [0x0006]}
           changes))))

(deftest test-bnz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  BNZ 06"
          "  LDI #34"]
         2)]
    (is (= {:D 0x12
            :R [0x0006]}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  BNZ 06"
          "  LDI #34"]
         3)]
    (is (= {:D 0x34
            :R [0x0006]}
           changes))))

(deftest test-lda
  (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDA 1"
          "fffe:"
          "  BYTE #12"]
         2)]
    (is (= {:D 0x12
            :R [5 0xffff]}
           changes)))
    (let [changes
        (run-prog
         ["  RLDI 1 #fffe"
          "  LDA 1"
          "  LDA 1"
          "fffe:"
          "  BYTE #12"
          "  BYTE #34"]
         3)]
    (is (= {:D 0x34
            :R [6]} ; R1 wraps over to zero
           changes))))

(deftest test-str
  (let [changes
        (run-prog
         ["  RLDI 1 #ffff"
          "  LDI #12"
          "  STR 1"])]
    (is (= {:D 0x12
            :R [7 0xffff]
            :mem {0xffff (mem-byte 0x12)}}
           changes))))

(deftest test-rlxa
  (let [changes
        (run-prog
         ["  RLDI 1 #0100"
          "  SEX 1"
          "  RLXA 2"
          "0100:"
          "  BYTE #12"
          "  BYTE #34"]
         3)]
    (is (= {:R [7 0x0102 0X1234]
            :X 1}
           changes))))

(deftest test-scal-sret
  (let [changes
        (run-prog
         ["  RLDI 2 #ffff"
          "  SEX 2"
          "  SCAL 1 0100"
          "  BYTE #03"
          "  RLDI 5 #0550"
          "0100:"
          "  LDA 1   ; should pick up the #03 at call site"
          "  PHI 3"
          "  SCAL 1 0200"
          "  LDI #30"
          "  PLO 3"
          "  SRET 1"
          "0200:"
          "  RLDI 4 #0440"
          "  SRET 1"]
         12)]
    (is (= {:R [14 nil 0xffff 0x0330 0x0440 0x0550]
            :D 0x30
            :X 2
            :mem {0xfffc (mem-byte 0x00)
                  0xfffd (mem-byte 0x0a)
                  0xfffe (mem-byte 0x00)
                  0xffff (mem-byte 0x00)}}
           changes))))

(deftest test-rsxd
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"
          "  RLDI 2 #ffff"
          "  SEX 2"
          "  RSXD 1"])]
    (is (= {:R [11 0x1234 0Xfffd]
            :X 2
            :mem {0xfffe (mem-byte 0x12)
                  0xffff (mem-byte 0x34)}}
           changes))))

(deftest test-rldi
  (let [changes
        (run-prog
         ["  RLDI 1 #1234"])]
    (is (= {:R [4 0x1234]}
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
            :R [0x0008 0x00ff]
            :mem {0x0100 (mem-byte 0x34)}}
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
            :R [0x000a 0xff12]}
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
            :R [0x000a 0x12ff]}
           changes))))

(deftest test-lbr
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LBR 2000"
          "  LDI #34"]
         2)]
    (is (= {:D 0x12
            :R [0x2000]}
           changes))))

(deftest test-nop
  (let [changes
        (run-prog
         ["  NOP"])]
    (is (= {:R [1]}
           changes))))

(deftest test-lsnz
  (let [changes
        (run-prog
         ["  LDI #12"
          "  LSNZ"
          "  LDI #34"]
         2)]
    (is (= {:D 0x12
            :R [0x0005]}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #00"
          "  LSNZ"
          "  LDI #34"])]
    (is (= {:D 0x34
            :R [0x0005]}
           changes))))

(deftest test-sep
  (let [changes
        (run-prog
         ["  LDI #12"
          "  RLDI 1 #0009"
          "  SEP 1"
          "  LDI #34"]
         3)]
    (is (= {:D 0x12
            :P 1
            :R [0x0007 0x0009]}
           changes))))

(deftest test-sex
  (let [changes
        (run-prog
         ["  SEX 1"])]
    (is (= {:X 1
            :R [0x0001]}
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
            :R [0x000b 0x0100]
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
            :R [0x000b 0x0100]
            :mem {0x0100 {:op :byte, :value 0x0f}}}
           changes))))

(deftest test-ldi
  (let [changes
        (run-prog
         ["  LDI #12"])]
    (is (= {:D 0x12
            :R [2]}
           changes))))

(deftest test-ori
  (let [changes
        (run-prog
         ["  LDI #0f"
          "  ORI #f0"])]
    (is (= {:D 0xff
            :R [4]}
           changes))))

(deftest test-ani
  (let [changes
        (run-prog
         ["  LDI #12"
          "  ANI #0f"])]
    (is (= {:D 0x02
            :R [4]}
           changes))))

(deftest test-xri
  (let [changes
        (run-prog
         ["  LDI #12"
          "  XRI #34"])]
    (is (= {:D 0x26
            :R [4]}
           changes))))

(deftest test-adi
  (let [changes
        (run-prog
         ["  LDI #34"
          "  ADI #12"])]
    (is (= {:D 0x46
            :R [4]}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #fe"
          "  ADI #03"])]
    (is (= {:D 0x01
            :DF 1
            :R [4]}
           changes))))

(deftest test-smi
  (let [changes
        (run-prog
         ["  LDI #34"
          "  SMI #12"])]
    (is (= {:D 0x22
            :DF 1
            :R [4]}
           changes)))
  (let [changes
        (run-prog
         ["  LDI #01"
          "  SMI #02"])]
    (is (= {:D 0xff
            :R [4]}
           changes))))

(deftest test-byte
  (let [changes
        (run-prog
         ["  BYTE #C0"
          "  BYTE #12"
          "  BYTE #34"]
         1)]
    (is (= {:R [0x1234]}
           changes))))

(deftest test-lisp
  (let [final-state (run (reader "()\r"))]
    (is (= " P-LISP FOR 1805 vers 1.0 210884\r\n C 1984 PERTTI KELLOMÄKI      \r\n CELLS FREE\r\nLISP RUNNING\r\n\r\n-NIL"
           ((:writer final-state))))))
