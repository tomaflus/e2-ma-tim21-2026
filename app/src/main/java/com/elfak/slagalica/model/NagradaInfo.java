package com.elfak.slagalica.model;

public class NagradaInfo {
    public final int tokeniNedelja;
    public final int pozicijaNedelja;
    public final int tokeniMesec;
    public final int pozicijaMesec;
    public final int ukupnoTokena;

    public NagradaInfo(int tokeniNedelja, int pozicijaNedelja,
                       int tokeniMesec, int pozicijaMesec) {
        this.tokeniNedelja = tokeniNedelja;
        this.pozicijaNedelja = pozicijaNedelja;
        this.tokeniMesec = tokeniMesec;
        this.pozicijaMesec = pozicijaMesec;
        this.ukupnoTokena = tokeniNedelja + tokeniMesec;
    }
}
