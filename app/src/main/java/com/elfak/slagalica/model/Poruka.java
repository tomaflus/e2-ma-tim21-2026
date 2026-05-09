package com.elfak.slagalica.model;

public class Poruka {
    private String id;
    private String posiljacId;
    private String posiljacIme;
    private String tekst;
    private long vrijemeSlanja;
    private String region;

    public Poruka() {}

    public Poruka(String posiljacId, String posiljacIme,
                  String tekst, String region) {
        this.posiljacId = posiljacId;
        this.posiljacIme = posiljacIme;
        this.tekst = tekst;
        this.region = region;
        this.vrijemeSlanja = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPosiljacId() { return posiljacId; }
    public void setPosiljacId(String posiljacId) { this.posiljacId = posiljacId; }

    public String getPosiljacIme() { return posiljacIme; }
    public void setPosiljacIme(String posiljacIme) { this.posiljacIme = posiljacIme; }

    public String getTekst() { return tekst; }
    public void setTekst(String tekst) { this.tekst = tekst; }

    public long getVrijemeSlanja() { return vrijemeSlanja; }
    public void setVrijemeSlanja(long vrijemeSlanja) { this.vrijemeSlanja = vrijemeSlanja; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}