6000:   
        LDI #60
        PHI 3
        LDI #07
        PLO 3
        SEP 3
        LDI #70
        PHI F
        LDI #09
        PLO F
        LDA F
        PHI 2
        LDI #FF
        PLO 2
        LDA F
        PHI 7
        LDI #00
        PLO 7
7009:
        BYTE #FF
        BYTE #E0
        BYTE #83
