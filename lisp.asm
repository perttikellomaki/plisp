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

7026:
        BYTE #01                ; FREE CELLS
        BYTE #00                ; todo: should be set by gc
7028:
        BYTE #83                ; LAST CELL USED + 04 (SEARCH START)
        BYTE #00                ; todo: should be set by gc

702C:
        BYTE #2F                ; READ SPECIAL CHR

;;; ATOM BUFF
;;; There is no surviving documentation, so this is a modern
;;; reconstruction.

7100:
        BYTE #71                ; 2 1ST BYTES = END OF LIB ADDR
        BYTE #1B

7102:   
        BYTE #00
        BYTE #00
        BYTE #04
        STRING "NIL"
7108:
        BYTE #00
        BYTE #04
        BYTE #02
        STRING "T"
710C:
        BYTE #80
        BYTE #00
        BYTE #05
        STRING "CONS"
7113:
        BYTE #80
        BYTE #0C
        BYTE #06
        STRING "QUOTE"
711B:   
        BYTE #FF

;;; The memory map indicates that cells for Lisp built ins
;;; are stored in a separate area.
;;; There is no surviving documentation, so this is a modern
;;; reconstruction.

8000:
        BYTE #00                ; CONS
        BYTE #0C                ; 0x000c indicates atom 
        BYTE #80
        BYTE #04
8004:
        BYTE #80                ; val
        BYTE #08
        BYTE #00                ; props
        BYTE #00
8008:
        BYTE #00                ; ml function CONS
        BYTE #10
        BYTE #67
        BYTE #FA
800C:
        BYTE #00                ; QUOTE
        BYTE #0C
        BYTE #80
        BYTE #10
8010:
        BYTE #80
        BYTE #14
        BYTE #00
        BYTE #00
8014:
        BYTE #00                ; ml function QUOTE
        BYTE #10
        BYTE #66
        BYTE #4F

;;; LISP-TULKKI

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

        SCAL 4 6511             ; EVAL

        SCAL 4 6442             ; PRINT (original calls 62ec but the is clearly a typo)

        BR B8
	
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

;;; NEWNODE
	
6232:   
	RSXD E                  ; SAVE REGS
        RSXD F

        RLDI 6 #7026            ; KENNOJA JÄLJELLÄ?
        LDA 6
        STR 2
        LDA 6
        OR
        BNZ 53                  ; JOS ON, HAE

        IDLE

        ;; The following is somewhat suspect. The datasheet gives
        ;; the semantics of RLXA as:
        ;;
        ;; M(R(X)) -> R(N).1
        ;; M(R(X)+1) -> R(N).0
        ;; R(X)+2 -> R(X)
        ;;
        ;; However, it is silent about what happens if R(X) and R(N)
        ;; are the same register. Presumably it works on real hardware,
        ;; and when it works it is an efficient way to follow a chain
        ;; of indirections.
6253:
        SEX 6                   ; HAE SEARCH START
        RLXA 6

        INC 6
        LDA 6
        INC 6
        INC 6                   ; KENNO VAPAA?
        ANI #01                 ; JOS ON, KÄYTÄ
        BNZ 60
        BR 56

6260:
        RLDI F #7029            ; RF OS SEARCH START

        SEX F                   ; VIE UUSI
        RSXD 6

        LDN F                   ; JA VÄHENNÄ KENNO
        SMI #01
        STXD
        LDN F
        SMBI #00
        STXD
        DEC 6

        SEX 2                   ; PALUU
        INC 2
        RLXA F
        RLXA E
        DEC 2
        SRET 4

;;; MAKELIST

629F:
        GHI E                   ; #ELS=0 ?
        BNZ AB
        GLO E
        BNZ AB

        LDI #00                 ; JOS ON, RET NIL
        PHI 6
        PLO 6
        SRET 4

        DEC E                   ; JOS EI, REKURSIO
        SCAL 4 629F

        GHI 6                   ; CDR PINOON
        STR 7
        INC 7
        GLO 6
        STR 7
        INC 7
62B6:
        SCAL 4 6232             ; NEWNODE

        DEC 7
        LDN 7
        PLO E
        DEC 7
        LDN 7
        PHI E

        GHI 8                   ; ERROR ?
        BNZ A5                  ; JOS ON, RET NIL

        SEX 6                   ; VIE CAR & CDR
        RSXD E                  ; UUTEEN KENNOON
        RSXD E
        SEX 2
        INC 6
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
        SMI #01                 ; LOPPUSULKU?
        BZ DA                   ; ON -> LISTREAD
        SMI #15                 ; > ?
        BZ DA                   ; ON -> LISTREAD
        ADI #02                 ; < ?
        BNZ 1F                  ; JOS EI, LUE ATOMI
6310:
        SCAL 4 63D9             ; CALL LISTREAD
        INC F

        GLO F                   ; PÄIVITÄ READ PTR
        STR 2
        LDI #7F
        PLO F
        LDN 2
        STR F
        PLO F
        SRET 4
631F:
        RLDI 6 #702C            ; HAE ERIKOISKOHTELUMERKKI AKKUUN
	LDN 6

        RLDI 6 #7100            ; R6 = STRING STOR.
6328:
        SEX 6                   ; R6 = END LIB
        RLXA 6
        SEX 2
        INC 6
        INC 6
        INC 6

        RSXD 6                  ; ATOMIN ALKUOS. PINOON
        STR 2                   ; ERIKOISKOHTELUMERKKI

        LDI #00                 ; RB.0 = CHR COUNT
        PLO B

        GHI 6
        XRI #80
        BNZ 40

633A:   
        LDI #05                 ; ERR: STRING STORAGE OVERFLOW
        PHI 8
        LBR 6417                ; RET NIL

6340:
        GLO B                   ; TOO LONG ATOM NAME?
        XRI #FE
	BZ 3A

        LDN F                   ; RIVIN LOPPU?
        BZ 71

        STR 6                   ; EI -> VIE MERKKI
        INC 6
        INC B
        INC F

        XOR                     ; ERIKOISMERKKI?
        BNZ 59

        DEC 6                   ; JOS ON, VIE SEUR. MERKKI
        LDN F
        LSNZ
        LDI #20
        STR 6
        INC 6
        INC F
        BR 35
6359:
        XOR                     ; PALAUTA MERKKI
        XRI #3E                 ; > ?
        BZ 6E
        XRI #02                 ; TAI < ?
        BZ 6E
        XRI #15                 ; TAI ) ?
        BZ 6E
        XRI #01                 ; TAI ( ?
        BZ 6E
        XRI #08                 ; TAI SPACE?
        BNZ 35                  ; JOS EI, JATKA SIIRTOA

        DEC F
        DEC B
        DEC 6

        LDI #FF                 ; KIRJASTON LOPPU
        STR 6
6374:
        INC 2                   ; RE = NIMEN ALKU
        RLXA E
        DEC 2
        

        NOP                     ; TODO: call numconv
        NOP
        NOP
        NOP

        RLDI A #7102            ; RA = STRING START
        SEX A
        INC B

        LDN A                   ; KIRJASTO LOPPU?
        XRI #FF
        BZ AC                   ; JOS ON, TEE UUSI ATOMI

        RLXA 6                  ; R6 = ATOMIN OS.
6389:
        GLO A                   ; RD OS. SEUR. ATOMI
        ADD
        PLO D
        GHI A
        ADCI #00
        PHI D

        GLO B                   ; PITUUS SAMA?
        XOR
        BNZ A5                  ; EI -> SEUR. ATOMI

        GHI E                   ; RC OS. NIMEÄ
        PHI C
        GLO E
        PLO C
        GLO B                   ; R8.0 = COUNT
        PLO 8

        DEC 8                   ; KAIKKI KIRJAIMET VERRATTU?
        GLO 8
        BZ A9                   ; ON -> ATOMI LÖYTYI
639E:
        INC A                   ; VERTAA
        LDA C
        XOR
        BNZ A5                  ; JOS EI SAMA, SIIRRY SEUR. ATOMIIN
        BR 9A

        RNX D                   ; SIIRRY SEUR. ATOMIIN
        BR 82

        SEX 2                   ; PALAA & PÄIVITÄ READ PTR
        BR 15

63AC:
        SEX 2
        RLDI E #0002            ; TEE UUSI ATOMI
        SCAL 4 629F

63B5:   
        GHI 8                   ; ERR?
        BNZ 3D                  ; JOS ON, RET NIL

        INC D
        INC D
        SEX D
        GLO B
        STR D                   ; VIE PITUUS

        GLO D                   ; RC = END LIB
        ADD
        PLO C
        GHI D
        ADCI #00
        PHI C

        DEC D                   ; VIE ATOMIN OSOITE
        RSXD 6

        RLDI D #7101            ; RD OS END LIB
63CB:
        RSXD C                  ; VIE UUSI END LIB

        SEX 6                   ; JA TEE ATOMI
        LDI #00
        STR 6
        INC 6
        LDI #0C
        STR 6
        DEC 6

        SEX 2                   ; JA PALAA
        BR 15

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

	SMI #15                 ; > ?
        BZ E4                   ; JOS ON, RET NIL

        SCAL 4 62F6             ; CALL READ

        GHI 6                   ; CAR TALTEEN
        STR 7
        INC 7
        GLO 6
        STR 7
        INC 7
63F5:
        SCAL 4 63DA             ; CALL LISTREAD

        GHI 6                   ; CDR TALTEEN
        STR 7
        INC 7
        GLO 6
        STR 7
        INC 7

        SCAL 4 6232             ; NEWNODE

        GHI 8                   ; ERR?
        BNZ 17                  ; JOS ON, RET NIL

        SEX 6                   ; VIE CAR & CDR
        DEC 7
        LDN 7
        STXD
        DEC 7
        LDN 7
        STXD
640D:
        DEC 7
        LDN 7
        STXD
        DEC 7
        LDN 7
        STR 6
        SEX 2
        LBR 6315

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
	STRING "NIL"
        BYTE #00
        SRET 4
6452:
        SCAL 4 60D1
        STRING "T"
        BYTE #00
        SRET 4

645A:
        LDA 6                   ; ATOMI ?
        BNZ A5                  ; JOS EI, PRINTTAA LISTA

        LDN 6                   ; CAR = T OR CAR = NIL
        BZ A5                   ; -> PRINT LISTA
        SMI #04
        BZ A5

        SMI #04                 ; NUMERO ?
        NOP                     ; original: LBZ
        NOP
        NOP

        SMI #04                 ; TUNNUS ?
        BNZ FB

        DEC 6                   ; ON -> LÄHDE ETSIMÄÄN
        RLDI A #7102
6472:
        SEX A

        LDN A
        XRI #FF                 ; KIRJASTO LOPPU?
        BNZ 7D

	;; The error code is missing in the original, but #10
        ;; is listed as "illegal atom".
        LDI #10                 ; JOS ON, ANNA ERR
        PHI 8
        BR 17

        GHI 6                   ; LÖYTYIKÖ ATOMI?
        XOR
        STR 2
        INC A
        GLO 6
        XOR
        SEX 2
        OR
        SEX A
        BZ 92                   ; JOS LÖYTYI, PRINTTAA
6488:
        INC A                   ; SEUR. ATOMI
        GLO A
        ADD
        PLO A
        GHI A
        ADCI #00
        PHI A
        BR 73

        SEX 2                   ; PRINTTAA
        INC A
        LDA A                   ; LASKURI PINOON
        STR 2

        LDN 2                   ; VÄHENNÄ
        SMI #01
        STR 2
        BZ 50                   ; JOS LASKURI = 00, LOPETA PRINTTAUS
649C:
        LDA A                   ; HAE CHR
        DEC 2
        SCAL 4 7003             ; PRINTTAA
        INC 2
        BR 96
64A5:
        DEC 6
        SCAL 4 60D1             ; LISTPRINT
        STRING "("              ; ALKUSULKU
        BYTE #00

        NOP                     ; original: SCAL 4 7006   ; BREAK?
        NOP
        NOP
        NOP
        GHI 8
        BZ B5                   ; JOS EI, JATKA
        SRET 4
64B5:
        SEX 6                   ; RA = CAR
        RLXA A
        SEX 2
        RSXD 6                  ; CDR PINOON
        GHI A
        PHI 6
        GLO A
        PLO 6
        SCAL 4 6442             ; PRINT CAR

        INC 2                   ; R6 = CDR
        RLXA 6
        DEC 2
        SEX 6
        RLXA 6
        SEX 2
        GHI 6                   ; JOS CDR = NIL, LOPETA
        BNZ D9
64CE:
        GLO 6
        BNZ ED                  ; CDR = T -> TULOASTA PIST. PARI

        SCAL 4 60D1             ; LOPPUSULKU
        STRING ")"
        BYTE #00
        SRET 4

        LDN 6                   ; CDR ATOMI?
        BNZ E5                  ; JOS EI, JATKA

        INC 6                   ; JOS NIL -> LISTA, CAR = NIL
        LDN 6
        DEC 6
        BZ E5
        SMI #04                 ; T -> LISTA
        BNZ ED                  ; EI -> PRINTTAA PIST. PARI
64E5:
        SCAL 4 60D1             ; VÄLISPACE
        STRING " "
        BYTE #00
        BR AC                   ; JA JATKA

        SCAL 4 60D1             ; PISTEELLINEN PARI
        STRING " . "
        BYTE #00

        SCAL 4 6442             ; PRINT CDR
        BR D1

;;; The addresses are a bit messed up in the original,
;;; but this is clearly the intent.
64FB:
        SCAL 4 60D1             ; ML-FN
        STRING "*ml-function"
        BYTE #00
        SRET 4

;;; EVAL

6511:
        GHI 7                   ; TILAA?
        STR 2
        GHI 2
        SMI #01
        XOR
        BNZ 1C

        LDI #08                 ; EI -> STACK OVERFLOW
        PHI 8

        RSXD 6                  ; SAVE EXPR

        GHI 8                   ; ERR -> DON'T EVAL
        BNZ 8E

        GHI 6                   ; T OR NIL?
        BZ 8E                   ; ON -> RET
6524:   
        LDA 6                   ; ATOMI ?
        BNZ 57                  ; EI -> EVALLIST

        LDN 6                   ; LISTA, CAR = NIL OR T ?
        BZ 2E
        SMI #04
        BNZ 33

        LDI #09                 ; JOS ON, ANNA ERR
        PHI 8
        BR 99

        DEC 6
        SMI #04                 ; NUMERO -> RETURN
        BZ 8E

        SMI #04                 ; TUNNUS
653A:
        BNZ 4A                  ; EI -> YLI

        INC 6
        INC 6
        SEX 6
        RLXA 6
        RLXA 6
        SEX 2
        GLO 6
        ANI #FC
        PLO 6
        BR 8E                   ; JA POIS

        SMI #04                 ; ML-FN ?
        BNZ 53

        INC 6                   ; KÄYNNISTÄ
        INC 6
        SEP 5
        BR 8E

6553:
        LDI #10                 ; ERR 10: ILLEGAL ATOM
        BR 30

        DEC 6                   ; EVALLIST
        SEX 6
        RLXA F                  ; GET CAR
        SEX 2
        RSXD F                  ; AND SAVE
        SEX 6
        RLXA F                  ; GET CDR
        SEX 2                   ; AND PUSH IT TO ARGSTACK
        GHI F
        STR 7
        INC 7
        GLO F
        STR 7
        INC 7

        INC 2                   ; GET CAR
        RLXA 6
        DEC 2
656C:
        SCAL 4 6511             ; EVAL

        GHI 6                   ; T OR NIL?
        BZ 7D                   ; JOS ON -> ERR
        LDN 6                   ; TULOS ATOMI?
        BNZ 81                  ; EI -> JATKA
        INC 6
        LDN 6
        DEC 6
        SMI #10                 ; ML-FN?
        BZ 81                   ; ON -> JATKA

        DEC 7                   ; ERROR
        DEC 7
        BR ED

        SCAL 4 6511             ; EVAL LOPPUTULOS

        DEC 7                   ; RETURN ARGSTACK
        DEC 7

        NOP                     ; BREAK?    original: SCAL 4 7006
        NOP
        NOP
        NOP

        GHI 8                   ; ERROR?
        BNZ 92                  ; JOS ON, ANNA MESS.

658E:
        INC 2                   ; JOS EI, PALAA
        INC 2
        SRET 4

        GHI 8                   ; JOS ERROR = Fx, EI PRINTATA
        ANI #F0
        XRI #F0
6597:
        BZ 8E

        IDLE

        
;;; ML-FN CALL

65F2:
        SEP 3
        RSXD 4                  ; ENTRY
        SEX 4
        RNX 3
        SEX 6
        RLXA 3
        SEX 2
        BR F2

;;; GETARG

65FE:
        DEC 7
        DEC 7
        SEX 7
        RLXA 6                  ; HAE ARG
        SEX 2
        RSXD 6                  ; TALLETA
6606:
        GHI 6                   ; NIL?
        BNZ 15                  ; EI -> JATKA

        RLDI 6 #7022            ; R6 OS FLAG
        LDN 6
        BZ 22                   ; JOS FLAG = 00, ÄLÄ ANNA ERRORIA

        LDI #06                 ; WRONG # ARGS
        PHI 8
        BR 22

        SEX 6                   ; VIE CDR ARGPINOON
        INC 6
        INC 6
        RLXA 6
        DEC 7
        SEX 7
        RSXD 6
661E:
        INC 7
        INC 7
        INC 7

        SEX 2                   ; PALAUTA CAR
        INC 2
        RLXA 6
        DEC 2
        GHI 6
        BZ 2D

        SEX 6                   ; JA HAE ARG
        RLXA 6
        SEX 2

        SRET 4

;;; EVALARG

662F:
        SCAL 4 65FE             ; GETARG
        SCAL 4 6511             ; EVAL
        SRET 4


;;; QUOTE

664F:
        SCAL 4 65FE             ; GETARG

;;; ARGCHK

6653:
        DEC 7                   ; ARG. PINOSSA NIL?
        DEC 7
        SEX 7
        RLXA F
        SEX 2
        GHI F
        BZ 5F

        LDI #06                 ; JOS EI, ANNA ERROR
        PHI 8

        SRET 4

;;; EVALTWO

66C6:
        SCAL 4 662F             ; EVALARG

        DEC 7                   ; HAE ARGLIST
        DEC 7
        SEX 7
        RLXA F
        DEC 7
        GLO 6                   ; CAR ARG PINOON
        STR 7
        DEC 7
        GHI 6
        STR 7
        INC 7
        INC 7
        GHI F                   ; REST OF ARGS PINOON
        STR 7
        INC 7
        GLO F
        STR 7
        INC 7
66DD:
        SEX 2
        SCAL 4 662F             ; EVALARG

        DEC 7                   ; RE = REST OF ARGS
        LDN 7
        PLO E
        DEC 7
        LDN 7
        PHI E

        DEC 7                   ; RF = 1ST ARG
        DEC 7
        SEX 7
        RLXA F

        SEX 2
        SRET 4

;;; (CONS X Y)

67FA:
        SCAL 4 6232             ; NEWNODE

        DEC 7                   ; ADDR -> ARGSTACK
        LDN 7
        PLO F
        GLO 6
        ANI #FC
        STR 7
6805:
        DEC 7
        LDN 7
        PHI F
        GHI 6
        STR 7

        INC 7                   ; (X Y) -> ARGSTACK
        INC 7
        GHI F
        STR 7
        INC 7
        GLO F
        STR 7
        INC 7

        SCAL 4 66C6             ; EVALTWO

        DEC 7
        DEC 7
        DEC 7
        DEC 7
681A:
        LDA 7                   ; RD = NEW NODE
        PHI D
        LDA 7
        ORI #03
        PLO D

        GHI 8                   ; ERR?
        BNZ 37                  ; ON -> POIS

        SEX D
        GLO 6
        STXD
        GHI 6
        STXD
        GLO F
        STXD
        GHI F
        STR D

        GHI D                   ; R6 = NEW NODE
        PHI 6
        GLO D
        PLO 6
6830:
        SEX 2
        GHI E
        BZ 37
        LDI #06
        PHI 8
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

;;; The call to this address expects to get a line of input into the
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
        BZ 1F

        LDN F
        XRI #04                 ; control-d ends input
        BNZ 1A

        IDLE                    ; stop the processor
        
        GLO F
        XRI #FE                 ; forced end of line if at end of buffer
        BNZ 0D

	LDI #20                 ; turn the carriage return into a space
        STR F
        INC F
        LDI #00                 ; mark end of line with #00
        STR F
        SRET 4
