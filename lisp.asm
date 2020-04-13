        ;; TYÖSIVU 7000 ->
        
7009:
        BYTE #FF                ; STACK PAGE (STACK=##FF)       OL. FF
        BYTE #E0                ; ARGSTACK PAGE (ARGSTACK=##00) OL. E0
        BYTE #83                ; LISP CELLS START PAGE         OL. 83
	
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
        LDI #00                 ; R9 = MACRO PC FOR 1802
        PHI 9                   ; (this was apparently never implemented)
        LDI #00
        PLO 9

        RLDI E #6032            ; RE OS. I/O ADDRS
        LDI #01                 ; RF OS. I/O HOOKS
        PLO F

