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

        ;; Original calls FREE, which is presumably same as GARBACOLL
        SCAL 4 6131

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

60b8:
        LDI #00                 ; TULKKAUSSILMUKKA
        PHI 8                   ; NO ERR
        SCAL 4 60D1
        BYTE #0D
        BYTE #0A
        BYTE #2D                ; the original code has #0D but that is clearly a typo
        BYTE #00
        SCAL 4 62EC             ; READ

        NOP                     ; SCAL 4 6511 ; EVAL
        NOP
        NOP
        NOP

        SCAL 4 6442             ; PRINT (original calls 62ec but the is clearly a typo)

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
        
;;; GARBACOLL
6131:
        RLDI F #700A            ; RF OS ARGSTACK PAGE
        LDA F
        STR 2
        LDN F
6138:
        PHI F                   ; RF OS LISP CELLS START
        LDI #00
        PLO F
613C:   
        INC F                   ; VAPAUTA KAIKKI SOLMUT
        LDN F
        ORI #01
        STR F
        INC F
        INC F
        INC F
        GHI F
        XOR
        BNZ 3C

        ;; todo: continue with marking
        SRET 4

;;; KILLSPACES

62D8:
        LDA F                   ; LUE MERKKI
        BNZ E5                  ; RIVIN LOPPU?
        SCAL 4 7000             ; JOS ON, LUE LISÄÄ
62DF:
        RLDI F #7080
        BR D8

        XRI #20
        BZ D8
        DEC F
        SRET 4

;;; READ
        
62EC:
        SCAL 4 7000             ; ENTRY FROM LISP

        RLDI F #707F            ; ML FN ENTRY
        LDN F
        PLO F                   ; RF OS. LUKEMISKOHTAA
62F6:
        SCAL 4 62D8             ; KILLSPACES

        LDN F                   ; LUE MERKKI
        SMI #27                 ; HEITTOMERKKI?

        NOP                     ; TODO: ;LBZ 6661
        NOP
        NOP

        SMI #01                 ; ALKUSULKU?
        BZ D9                   ; ON -> LISTREAD

	IDLE

;;; LISTREAD

63D9:   
        INC F                   ; SKIP ALKUSULKU
        SCAL 4 62D8             ; KILLSPACES
63DE:
        LDN F                   ; )?
        SMI #29
        BNZ E7

        INC F                   ; JOS ON, RET NIL
        LBR 6417

;;; NILRET

6417:
        SEX 2
        RLDI 6 #0000
        SRET 4

;;; PRINTSUB

6442:
        GHI 6                   ; T OR NIL
        BNZ 5A                  ; EI -> YLI
6445:
        GLO 6                   ; JOS ON, KUMPI?
        BNZ 52

        SCAL 4 60D1
        BYTE #4E                ; N
        BYTE #49                ; I
        BYTE #4C                ; L
        BYTE #00
        SRET 4
6452:
        SCAL 4 60D1
        BYTE #54                ; T
        BYTE #00
        SRET 4

;;; The code has references to I/O code at E9 and E7 pages.
;;; This does not show up in the memory map and there is no
;;; surviving documentation. It is replaced here with pseudo
;;; operations PRINTCHAR and READCHAR.

E72F:
        SRET 4
E731:
        PRINTCHAR
        SRET 4

;;; The call to this address expects to get a line of input in the
;;; read buffer in work page. The code here approximates what would
;;; have been in the original.
        
E906:
        RLDI F #707F            ; read buffer pointer
        LDI #80
        STR F
        
        INC F
        READCHAR
        STR F
        XRI #0D                 ; a carriage return ends line
        BZ 19
        GLO F
        XRI #FF                 ; forced end of line if at end of buffer
        BNZ 0D

        LDI #00                 ; mark end of line with #00
        STR F
        SRET 4
