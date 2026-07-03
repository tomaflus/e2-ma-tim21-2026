package com.elfak.slagalica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Turnir {
    private String id; // set locally after fetch, not stored in Firestore
    private String naziv;
    private String vlasnikId;
    private String vlasnikIme;
    private List<Map<String, Object>> igraci = new ArrayList<>();
    private String status; // "CEKA", "U_TOKU", "ZAVRSEN"
    private long createdAt;

    // Zreb — popunjavaju se kada turnir počinje (status → U_TOKU)
    private Map<String, Object> sf1Igrac1;
    private Map<String, Object> sf1Igrac2;
    private Map<String, Object> sf2Igrac1;
    private Map<String, Object> sf2Igrac2;
    private long sf1Bodovi1;
    private long sf1Bodovi2;
    private long sf2Bodovi1;
    private long sf2Bodovi2;

    // Finale — popunjava se nakon što su oba polufinala odigrana
    private Map<String, Object> finalIgrac1;
    private Map<String, Object> finalIgrac2;
    private long finalBodovi1;
    private long finalBodovi2;
    private String pobjednikIme;
    private String pobjednikId;
    private String drugiIme;
    private String drugiId;

    // 3. mjesto — popunjava se gubitnicima polufinala
    private Map<String, Object> treceIgrac1;
    private Map<String, Object> treceIgrac2;
    private long treceBodovi1;
    private long treceBodovi2;
    private String treciIme;
    private String treciId;
    private String cetvrtiIme;
    private String cetvrtiId;

    public Turnir() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }
    public String getVlasnikId() { return vlasnikId; }
    public void setVlasnikId(String vlasnikId) { this.vlasnikId = vlasnikId; }
    public String getVlasnikIme() { return vlasnikIme; }
    public void setVlasnikIme(String vlasnikIme) { this.vlasnikIme = vlasnikIme; }
    public List<Map<String, Object>> getIgraci() { return igraci != null ? igraci : new ArrayList<>(); }
    public void setIgraci(List<Map<String, Object>> igraci) { this.igraci = igraci; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getSf1Igrac1() { return sf1Igrac1; }
    public void setSf1Igrac1(Map<String, Object> sf1Igrac1) { this.sf1Igrac1 = sf1Igrac1; }
    public Map<String, Object> getSf1Igrac2() { return sf1Igrac2; }
    public void setSf1Igrac2(Map<String, Object> sf1Igrac2) { this.sf1Igrac2 = sf1Igrac2; }
    public Map<String, Object> getSf2Igrac1() { return sf2Igrac1; }
    public void setSf2Igrac1(Map<String, Object> sf2Igrac1) { this.sf2Igrac1 = sf2Igrac1; }
    public Map<String, Object> getSf2Igrac2() { return sf2Igrac2; }
    public void setSf2Igrac2(Map<String, Object> sf2Igrac2) { this.sf2Igrac2 = sf2Igrac2; }
    public long getSf1Bodovi1() { return sf1Bodovi1; }
    public void setSf1Bodovi1(long sf1Bodovi1) { this.sf1Bodovi1 = sf1Bodovi1; }
    public long getSf1Bodovi2() { return sf1Bodovi2; }
    public void setSf1Bodovi2(long sf1Bodovi2) { this.sf1Bodovi2 = sf1Bodovi2; }
    public long getSf2Bodovi1() { return sf2Bodovi1; }
    public void setSf2Bodovi1(long sf2Bodovi1) { this.sf2Bodovi1 = sf2Bodovi1; }
    public long getSf2Bodovi2() { return sf2Bodovi2; }
    public void setSf2Bodovi2(long sf2Bodovi2) { this.sf2Bodovi2 = sf2Bodovi2; }
    public Map<String, Object> getFinalIgrac1() { return finalIgrac1; }
    public void setFinalIgrac1(Map<String, Object> finalIgrac1) { this.finalIgrac1 = finalIgrac1; }
    public Map<String, Object> getFinalIgrac2() { return finalIgrac2; }
    public void setFinalIgrac2(Map<String, Object> finalIgrac2) { this.finalIgrac2 = finalIgrac2; }
    public long getFinalBodovi1() { return finalBodovi1; }
    public void setFinalBodovi1(long finalBodovi1) { this.finalBodovi1 = finalBodovi1; }
    public long getFinalBodovi2() { return finalBodovi2; }
    public void setFinalBodovi2(long finalBodovi2) { this.finalBodovi2 = finalBodovi2; }
    public String getPobjednikIme() { return pobjednikIme; }
    public void setPobjednikIme(String pobjednikIme) { this.pobjednikIme = pobjednikIme; }
    public String getPobjednikId() { return pobjednikId; }
    public void setPobjednikId(String pobjednikId) { this.pobjednikId = pobjednikId; }
    public String getDrugiIme() { return drugiIme; }
    public void setDrugiIme(String drugiIme) { this.drugiIme = drugiIme; }
    public String getDrugiId() { return drugiId; }
    public void setDrugiId(String drugiId) { this.drugiId = drugiId; }

    public Map<String, Object> getTreceIgrac1() { return treceIgrac1; }
    public void setTreceIgrac1(Map<String, Object> treceIgrac1) { this.treceIgrac1 = treceIgrac1; }
    public Map<String, Object> getTreceIgrac2() { return treceIgrac2; }
    public void setTreceIgrac2(Map<String, Object> treceIgrac2) { this.treceIgrac2 = treceIgrac2; }
    public long getTreceBodovi1() { return treceBodovi1; }
    public void setTreceBodovi1(long treceBodovi1) { this.treceBodovi1 = treceBodovi1; }
    public long getTreceBodovi2() { return treceBodovi2; }
    public void setTreceBodovi2(long treceBodovi2) { this.treceBodovi2 = treceBodovi2; }
    public String getTreciIme() { return treciIme; }
    public void setTreciIme(String treciIme) { this.treciIme = treciIme; }
    public String getTreciId() { return treciId; }
    public void setTreciId(String treciId) { this.treciId = treciId; }
    public String getCetvrtiIme() { return cetvrtiIme; }
    public void setCetvrtiIme(String cetvrtiIme) { this.cetvrtiIme = cetvrtiIme; }
    public String getCetvrtiId() { return cetvrtiId; }
    public void setCetvrtiId(String cetvrtiId) { this.cetvrtiId = cetvrtiId; }
}
