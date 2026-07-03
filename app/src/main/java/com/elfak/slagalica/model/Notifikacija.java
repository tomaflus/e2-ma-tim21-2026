package com.elfak.slagalica.model;

public class Notifikacija {
    public String dokumentId; // postavlja se lokalno nakon fetchovanja, nije u Firestoreu
    public String ikona;
    public String tip;
    public String naslov;
    public String sadrzaj;
    public String datumVrijeme;
    public boolean procitana;
    public String turnirId; // za tip "turnir" — navigacija na konkretan turnir

    // Strukturna polja za tip "nagrade" (ostala polja su 0 / -1)
    public int tokeniNedelja;
    public int pozicijaNedelja = -1;
    public int tokeniMesec;
    public int pozicijaMesec = -1;

    public Notifikacija() {}

    public Notifikacija(String ikona, String naslov, String sadrzaj,
                        String datumVrijeme, boolean procitana) {
        this.ikona = ikona;
        this.naslov = naslov;
        this.sadrzaj = sadrzaj;
        this.datumVrijeme = datumVrijeme;
        this.procitana = procitana;
    }

    public Notifikacija(String ikona, String tip, String naslov, String sadrzaj,
                        String datumVrijeme, boolean procitana,
                        int tokeniNedelja, int pozicijaNedelja,
                        int tokeniMesec, int pozicijaMesec) {
        this.ikona = ikona;
        this.tip = tip;
        this.naslov = naslov;
        this.sadrzaj = sadrzaj;
        this.datumVrijeme = datumVrijeme;
        this.procitana = procitana;
        this.tokeniNedelja = tokeniNedelja;
        this.pozicijaNedelja = pozicijaNedelja;
        this.tokeniMesec = tokeniMesec;
        this.pozicijaMesec = pozicijaMesec;
    }
}
