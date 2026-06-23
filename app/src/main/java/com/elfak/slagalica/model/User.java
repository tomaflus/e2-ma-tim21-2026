package com.elfak.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String email;
    private String korisnickoIme;
    private String region;
    private String avatarUrl;
    private int tokeni;
    private int zvezde;
    private int liga;
    private int mesecniBodovi;
    private long zadnjiLoginDatum;
    private boolean online;
    private int nedeljneZvezde;
    private int mesecneZvezde;
    private String nedeljaCiklusId = "";
    private String mesecCiklusId = "";
    private boolean rangiranNedelja;
    private boolean rangiranMesec;
    private String lastNagradaNedeljaId = "";
    private String lastNagradaMesecId = "";
    private List<String> prijateljiIds = new ArrayList<>();

    public User() {}

    public User(String id, String email, String korisnickoIme, String region) {
        this.id = id;
        this.email = email;
        this.korisnickoIme = korisnickoIme;
        this.region = region;
        this.tokeni = 5;
        this.zvezde = 0;
        this.liga = 0;
        this.zadnjiLoginDatum = System.currentTimeMillis();
        this.online = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getKorisnickoIme() { return korisnickoIme; }
    public void setKorisnickoIme(String korisnickoIme) { this.korisnickoIme = korisnickoIme; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getTokeni() { return tokeni; }
    public void setTokeni(int tokeni) { this.tokeni = tokeni; }
    public int getZvezde() { return zvezde; }
    public void setZvezde(int zvezde) { this.zvezde = zvezde; }
    public int getLiga() { return liga; }
    public void setLiga(int liga) { this.liga = liga; }
    public int getMesecniBodovi() { return mesecniBodovi; }
    public void setMesecniBodovi(int mesecniBodovi) { this.mesecniBodovi = mesecniBodovi; }
    public List<String> getPrijateljiIds() { return prijateljiIds; }
    public void setPrijateljiIds(List<String> prijateljiIds) { this.prijateljiIds = prijateljiIds; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getZadnjiLoginDatum() { return zadnjiLoginDatum; }
    public void setZadnjiLoginDatum(long zadnjiLoginDatum) { this.zadnjiLoginDatum = zadnjiLoginDatum; }
    public int getNedeljneZvezde() { return nedeljneZvezde; }
    public void setNedeljneZvezde(int nedeljneZvezde) { this.nedeljneZvezde = nedeljneZvezde; }
    public int getMesecneZvezde() { return mesecneZvezde; }
    public void setMesecneZvezde(int mesecneZvezde) { this.mesecneZvezde = mesecneZvezde; }
    public String getNedeljaCiklusId() { return nedeljaCiklusId; }
    public void setNedeljaCiklusId(String nedeljaCiklusId) { this.nedeljaCiklusId = nedeljaCiklusId; }
    public String getMesecCiklusId() { return mesecCiklusId; }
    public void setMesecCiklusId(String mesecCiklusId) { this.mesecCiklusId = mesecCiklusId; }
    public boolean isRangiranNedelja() { return rangiranNedelja; }
    public void setRangiranNedelja(boolean rangiranNedelja) { this.rangiranNedelja = rangiranNedelja; }
    public boolean isRangiranMesec() { return rangiranMesec; }
    public void setRangiranMesec(boolean rangiranMesec) { this.rangiranMesec = rangiranMesec; }
    public String getLastNagradaNedeljaId() { return lastNagradaNedeljaId != null ? lastNagradaNedeljaId : ""; }
    public void setLastNagradaNedeljaId(String id) { this.lastNagradaNedeljaId = id; }
    public String getLastNagradaMesecId() { return lastNagradaMesecId != null ? lastNagradaMesecId : ""; }
    public void setLastNagradaMesecId(String id) { this.lastNagradaMesecId = id; }
}