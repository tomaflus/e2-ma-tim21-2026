package com.elfak.slagalica.model;

public class Partija {
    private String id;
    private String igrac1Id;
    private String igrac2Id;
    private String igrac1Ime;
    private String igrac2Ime;
    private StatusPartije status;
    private int trenutnaIgra; // 0-5 (6 igara)
    private int bodovi1;
    private int bodovi2;
    private String pobjednik;
    private boolean prijateljska;
    private long kreirano;

    public Partija() {}

    public Partija(String igrac1Id, String igrac1Ime, boolean prijateljska) {
        this.igrac1Id = igrac1Id;
        this.igrac1Ime = igrac1Ime;
        this.status = StatusPartije.CEKANJE;
        this.trenutnaIgra = 0;
        this.bodovi1 = 0;
        this.bodovi2 = 0;
        this.prijateljska = prijateljska;
        this.kreirano = System.currentTimeMillis();
    }

    // Getteri i setteri
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIgrac1Id() { return igrac1Id; }
    public void setIgrac1Id(String igrac1Id) { this.igrac1Id = igrac1Id; }

    public String getIgrac2Id() { return igrac2Id; }
    public void setIgrac2Id(String igrac2Id) { this.igrac2Id = igrac2Id; }

    public String getIgrac1Ime() { return igrac1Ime; }
    public void setIgrac1Ime(String igrac1Ime) { this.igrac1Ime = igrac1Ime; }

    public String getIgrac2Ime() { return igrac2Ime; }
    public void setIgrac2Ime(String igrac2Ime) { this.igrac2Ime = igrac2Ime; }

    public StatusPartije getStatus() { return status; }
    public void setStatus(StatusPartije status) { this.status = status; }

    public int getTrenutnaIgra() { return trenutnaIgra; }
    public void setTrenutnaIgra(int trenutnaIgra) { this.trenutnaIgra = trenutnaIgra; }

    public int getBodovi1() { return bodovi1; }
    public void setBodovi1(int bodovi1) { this.bodovi1 = bodovi1; }

    public int getBodovi2() { return bodovi2; }
    public void setBodovi2(int bodovi2) { this.bodovi2 = bodovi2; }

    public String getPobjednik() { return pobjednik; }
    public void setPobjednik(String pobjednik) { this.pobjednik = pobjednik; }

    public boolean isPrijateljska() { return prijateljska; }
    public void setPrijateljska(boolean prijateljska) { this.prijateljska = prijateljska; }

    public long getKreirano() { return kreirano; }
    public void setKreirano(long kreirano) { this.kreirano = kreirano; }
}