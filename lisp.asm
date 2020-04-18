;;; P-LISP vers. 1.0 for the RCA 1805, 1984-08-20 onwards
;;; Originally written on paper in pencil and entered in hex by hand.
;;; This is a third version of the Lisp, I think. There is an earlier
;;; version also labeled 1.0 from early 1984, but for some reason I
;;; decided to start over. I seem to remember an even earlier version,
;;; but no documentation survives.
;;;
;;; Original comments are in capitals, some modern commentary (like this)
;;; in mixed case.

        ;; TYÖSIVU 7000 ->

7000:
;;; The nine bytes that follow are three long branches in the 1805
;;; assembly (opcode 0xC0). The initialization code copies default
;;; addresses in place of zeros. I think the rationale was that by
;;; changing the addresses was that I could then e.g. redirect output
;;; to the Teletype printer I had.
        BYTE #C0                ; INPUT HOOK
        BYTE #00
        BYTE #00
        BYTE #C0                ; OUTPUT HOOK
        BYTE #00
        BYTE #00
        BYTE #C0                ; BREAK
        BYTE #00
        BYTE #00

7009:
        BYTE #FF                ; STACK PAGE (STACK = ##FF)       OL. FF
        BYTE #E0                ; ARGSTACK PAGE (ARGSTACK = ##00) OL. E0
        BYTE #83                ; LISP CELLS START PAGE           OL. 83

;;; The following are formatted as Lisp integers (tag #0008).
        
700C:
        BYTE #00                ; XPOS
        BYTE #08
        BYTE #00
        BYTE #00
7010:
        BYTE #00                ; YPOS
        BYTE #08
        BYTE #00
        BYTE #00
7014:
        BYTE #00                ; TABS
        BYTE #08
        BYTE #00
        BYTE #00
7018:
        BYTE #00                ; OLD-X
        BYTE #08
        BYTE #00
        BYTE #00
701C:
        BYTE #00                ; OLD-Y
        BYTE #08
        BYTE #00
        BYTE #00

        ;; LISP-TULKKI

6000:   
        LDI #60                 ; P=3
        PHI 3
        LDI #07
        PLO 3
        SEP 3
        LDI #70
        PHI F                   ; RF OS. WP
        LDI #09
        PLO F
        
        LDA F                   ; X=2
        PHI 2
        LDI #FF
        PLO 2
        
        LDA F                   ; ARGSTACK PAGE
        PHI 7
        LDI #00
        PLO 7

6017:
;;; I was fortunate enough to have a RCA 1805 processor with an extended
;;; instruction set. The Lisp interpreter is written using the extended
;;; instructions, but my intention was to provide subroutines in 1802
;;; assembly that would implement the 1805 instructions. That support
;;; never materialized, however.
        LDI #00                 ; R9 = MACRO PC FOR 1802
        PHI 9
        LDI #00
        PLO 9

        RLDI E #6032            ; RE OS. I/O ADDRS
        LDI #01                 ; RF OS. I/O HOOKS
        PLO F

6024:   
        LDA E
        STR F
        INC F
        LDA E
        STR F
        INC F
        INC F
        GLO F
        XRI #0A
        BNZ 24

6030:
        BR 38

6032:
        BYTE #E9                ; INPUT
        BYTE #06
        BYTE #E7                ; OUTPUT
        BYTE #31
        BYTE #00                ; BREAK (actual value missing in listing)
        BYTE #00

6038:
        SEX 2                   ; CURRENT PROG=00
        LDI #22
        PLO f
        LDI #00
        STR F
        INC F
        STR F
        
        SCAL 4 E72F    ; I/O INIT
        RLDI 5 #65F3            ; R5 = ML-FN CALL

6049:
        SCAL 4 60D1             ; PROMPT
        
        STRING " P-LISP FOR 1805 vers 1.0 210884"
        BYTE #0D
        BYTE #0A
        STRING " C 1984 PERTTI KELLOMÄKI      "
        BYTE #0D
        BYTE #0A
        BYTE #00

        ;; original calls FREE
        ;; SCAL 4 XXXX
        NOP
        NOP
        NOP
        NOP

        ;; original calls PRINT # FREE CELLS
        ;; SCAL 4 XXXX
        NOP
        NOP
        NOP
        NOP

        ;; The original has SCAL 4 6091 but that is clearly a bug.
        SCAL 4 60D1             ; PROMPT
        STRING " CELLS FREE"
        BYTE #0D
        BYTE #0A
        STRING "LISP RUNNING"
        BYTE #0D
        BYTE #0A
        BYTE #00
        
       IDLE                    ; Not in original code, here to stop processor simulation.
;;; PROMPT

60D1:
        LDA 4
        BZ DA
        SCAL 4 60DC
        BR D1
        SRET 4

;;; OUT: UPDATE TABS

60DC:
        STR 2                   ; SAVE d
        RLDI F #7017            ; RF OS TABS
        SMI #08                 ; BS?
        BNZ EA
        LDN F
        SMI #01
60E8:
        BR F5

        SMI #04                 ; CLS?
        BZ F5
        SMI #01                 ; CR?
        BZ F5

        LDN F                   ; INC TABS
        ADI #01
        STR F                   ; VIE TABS

        LDN 2
        LBR 7003
        


;;; The code has references to I/O code at E9 and E7 pages.
;;; This does not show up in the memory map and there is no
;;; surviving documentation. It is replaced here with pseudo
;;; operations PRINTCHAR and READCHAR.

E72F:
        SRET 4
E731:
        PRINTCHAR
        SRET 4

