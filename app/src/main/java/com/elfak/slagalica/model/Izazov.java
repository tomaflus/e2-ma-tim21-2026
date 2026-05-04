package com.elfak.slagalica.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Izazov {
    private String id;
    private String kreatorId;
    private String kreatorIme;
    private String region;
    private int zvezdeUlog;
    private int tokeniUlog;
    private String status;
    private List<String> igraciIds;
    private List<String> igraciImena;
    private Map<String, Integer> rezultati;
    private long kreirano;

    public Izazov() {}

    public Izazov(String kreatorId, String kreatorIme, String region,
                  int zvezdeUlog, int tokeniUlog) {
        this.kreatorId = kreatorId;
        this.kreatorIme = kreatorIme;
        this.region = region;
        this.zvezdeUlog = zvezdeUlog;
        this.tokeniUlog = tokeniUlog;
        this.status = "AKTIVAN";
        this.igraciIds = new ArrayList<>();
        this.igraciIds.add(kreatorId);
        this.igraciImena = new ArrayList<>();
        this.igraciImena.add(kreatorIme);
        this.rezultati = new HashMap<>();
        this.kreirano = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKreatorId() { return kreatorId; }
    public void setKreatorId(String kreatorId) { this.kreatorId = kreatorId; }

    public String getKreatorIme() { return kreatorIme; }
    public void setKreatorIme(String kreatorIme) { this.kreatorIme = kreatorIme; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public int getZvezdeUlog() { return zvezdeUlog; }
    public void setZvezdeUlog(int zvezdeUlog) { this.zvezdeUlog = zvezdeUlog; }

    public int getTokeniUlog() { return tokeniUlog; }
    public void setTokeniUlog(int tokeniUlog) { this.tokeniUlog = tokeniUlog; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getIgraciIds() { return igraciIds; }
    public void setIgraciIds(List<String> igraciIds) { this.igraciIds = igraciIds; }

    public List<String> getIgraciImena() { return igraciImena; }
    public void setIgraciImena(List<String> igraciImena) { this.igraciImena = igraciImena; }

    public Map<String, Integer> getRezultati() { return rezultati; }
    public void setRezultati(Map<String, Integer> rezultati) { this.rezultati = rezultati; }

    public long getKreirano() { return kreirano; }
    public void setKreirano(long kreirano) { this.kreirano = kreirano; }
}