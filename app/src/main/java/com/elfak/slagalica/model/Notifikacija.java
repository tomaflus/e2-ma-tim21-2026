package com.elfak.slagalica.model;

public class Notifikacija {
    public String ikona;
    public String naslov;
    public String sadrzaj;
    public String datumVrijeme;
    public boolean procitana;

    public Notifikacija(String ikona, String naslov, String sadrzaj,
                        String datumVrijeme, boolean procitana) {
        this.ikona = ikona;
        this.naslov = naslov;
        this.sadrzaj = sadrzaj;
        this.datumVrijeme = datumVrijeme;
        this.procitana = procitana;
    }
}
