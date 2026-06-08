package com.elfak.slagalica.model;

import java.util.List;

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

    private String statusKorakPoKorak; // IGRAC1_IGRA, IGRAC2_IGRA, ZAVRSENA
    private int bodovi1KorakPoKorak;
    private int bodovi2KorakPoKorak;
    private boolean igrac1PogodioKorak;
    private boolean igrac2PogodioKorak;
    private String pitanjeKorakPoKorakId;

    private String statusMojBroj;
    private int bodovi1MojBroj;
    private int bodovi2MojBroj;
    private int ciljniBrojRunda1;
    private int ciljniBrojRunda2;
    private List<Integer> dostupniBrojeviRunda1;
    private List<Integer> dostupniBrojeviRunda2;
    private int rezultat1Runda1;
    private int rezultat2Runda1;
    private int rezultat1Runda2;
    private int rezultat2Runda2;

    // Skočko
    private String statusSkocko;
    private String tajnaKombinacijaR1Skocko;
    private String tajnaKombinacijaR2Skocko;
    private int bodovi1Skocko;
    private int bodovi2Skocko;

    // Asocijacije
    private String statusAsocijacije;
    private String pitanjeAsocijacijeIdR1;
    private String pitanjeAsocijacijeIdR2;
    private List<Boolean> otvorenaPoljaR1;
    private List<Boolean> pogodeneKoloneR1;
    private boolean konacnoPogodenoR1;
    private List<Boolean> otvorenaPoljaR2;
    private List<Boolean> pogodeneKoloneR2;
    private boolean konacnoPogodenoR2;
    private int bodovi1Asocijacije;
    private int bodovi2Asocijacije;
    private long timerEndMsAsocijacijeR1;
    private long timerEndMsAsocijacijeR2;

    private String napustioId;

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

    public String getStatusKorakPoKorak() { return statusKorakPoKorak; }
    public void setStatusKorakPoKorak(String s) { this.statusKorakPoKorak = s; }

    public int getBodovi1KorakPoKorak() { return bodovi1KorakPoKorak; }
    public void setBodovi1KorakPoKorak(int b) { this.bodovi1KorakPoKorak = b; }

    public int getBodovi2KorakPoKorak() { return bodovi2KorakPoKorak; }
    public void setBodovi2KorakPoKorak(int b) { this.bodovi2KorakPoKorak = b; }

    public boolean isIgrac1PogodioKorak() { return igrac1PogodioKorak; }
    public void setIgrac1PogodioKorak(boolean b) { this.igrac1PogodioKorak = b; }

    public boolean isIgrac2PogodioKorak() { return igrac2PogodioKorak; }
    public void setIgrac2PogodioKorak(boolean b) { this.igrac2PogodioKorak = b; }

    public String getPitanjeKorakPoKorakId() { return pitanjeKorakPoKorakId; }
    public void setPitanjeKorakPoKorakId(String s) { this.pitanjeKorakPoKorakId = s; }

    public String getStatusMojBroj() { return statusMojBroj; }
    public void setStatusMojBroj(String s) { this.statusMojBroj = s; }

    public int getBodovi1MojBroj() { return bodovi1MojBroj; }
    public void setBodovi1MojBroj(int b) { this.bodovi1MojBroj = b; }

    public int getBodovi2MojBroj() { return bodovi2MojBroj; }
    public void setBodovi2MojBroj(int b) { this.bodovi2MojBroj = b; }

    public int getCiljniBrojRunda1() { return ciljniBrojRunda1; }
    public void setCiljniBrojRunda1(int c) { this.ciljniBrojRunda1 = c; }

    public int getCiljniBrojRunda2() { return ciljniBrojRunda2; }
    public void setCiljniBrojRunda2(int c) { this.ciljniBrojRunda2 = c; }

    public List<Integer> getDostupniBrojeviRunda1() { return dostupniBrojeviRunda1; }
    public void setDostupniBrojeviRunda1(List<Integer> d) { this.dostupniBrojeviRunda1 = d; }

    public List<Integer> getDostupniBrojeviRunda2() { return dostupniBrojeviRunda2; }
    public void setDostupniBrojeviRunda2(List<Integer> d) { this.dostupniBrojeviRunda2 = d; }

    public int getRezultat1Runda1() { return rezultat1Runda1; }
    public void setRezultat1Runda1(int r) { this.rezultat1Runda1 = r; }

    public int getRezultat2Runda1() { return rezultat2Runda1; }
    public void setRezultat2Runda1(int r) { this.rezultat2Runda1 = r; }

    public int getRezultat1Runda2() { return rezultat1Runda2; }
    public void setRezultat1Runda2(int r) { this.rezultat1Runda2 = r; }

    public int getRezultat2Runda2() { return rezultat2Runda2; }
    public void setRezultat2Runda2(int r) { this.rezultat2Runda2 = r; }
    public String getNapustioId() { return napustioId; }
    public void setNapustioId(String napustioId) { this.napustioId = napustioId; }

    public String getStatusSkocko() { return statusSkocko; }
    public void setStatusSkocko(String s) { this.statusSkocko = s; }

    public String getTajnaKombinacijaR1Skocko() { return tajnaKombinacijaR1Skocko; }
    public void setTajnaKombinacijaR1Skocko(String s) { this.tajnaKombinacijaR1Skocko = s; }

    public String getTajnaKombinacijaR2Skocko() { return tajnaKombinacijaR2Skocko; }
    public void setTajnaKombinacijaR2Skocko(String s) { this.tajnaKombinacijaR2Skocko = s; }

    public int getBodovi1Skocko() { return bodovi1Skocko; }
    public void setBodovi1Skocko(int b) { this.bodovi1Skocko = b; }

    public int getBodovi2Skocko() { return bodovi2Skocko; }
    public void setBodovi2Skocko(int b) { this.bodovi2Skocko = b; }

    public String getStatusAsocijacije() { return statusAsocijacije; }
    public void setStatusAsocijacije(String s) { this.statusAsocijacije = s; }

    public String getPitanjeAsocijacijeIdR1() { return pitanjeAsocijacijeIdR1; }
    public void setPitanjeAsocijacijeIdR1(String s) { this.pitanjeAsocijacijeIdR1 = s; }

    public String getPitanjeAsocijacijeIdR2() { return pitanjeAsocijacijeIdR2; }
    public void setPitanjeAsocijacijeIdR2(String s) { this.pitanjeAsocijacijeIdR2 = s; }

    public List<Boolean> getOtvorenaPoljaR1() { return otvorenaPoljaR1; }
    public void setOtvorenaPoljaR1(List<Boolean> l) { this.otvorenaPoljaR1 = l; }

    public List<Boolean> getPogodeneKoloneR1() { return pogodeneKoloneR1; }
    public void setPogodeneKoloneR1(List<Boolean> l) { this.pogodeneKoloneR1 = l; }

    public boolean isKonacnoPogodenoR1() { return konacnoPogodenoR1; }
    public void setKonacnoPogodenoR1(boolean b) { this.konacnoPogodenoR1 = b; }

    public List<Boolean> getOtvorenaPoljaR2() { return otvorenaPoljaR2; }
    public void setOtvorenaPoljaR2(List<Boolean> l) { this.otvorenaPoljaR2 = l; }

    public List<Boolean> getPogodeneKoloneR2() { return pogodeneKoloneR2; }
    public void setPogodeneKoloneR2(List<Boolean> l) { this.pogodeneKoloneR2 = l; }

    public boolean isKonacnoPogodenoR2() { return konacnoPogodenoR2; }
    public void setKonacnoPogodenoR2(boolean b) { this.konacnoPogodenoR2 = b; }

    public int getBodovi1Asocijacije() { return bodovi1Asocijacije; }
    public void setBodovi1Asocijacije(int b) { this.bodovi1Asocijacije = b; }

    public int getBodovi2Asocijacije() { return bodovi2Asocijacije; }
    public void setBodovi2Asocijacije(int b) { this.bodovi2Asocijacije = b; }

    public long getTimerEndMsAsocijacijeR1() { return timerEndMsAsocijacijeR1; }
    public void setTimerEndMsAsocijacijeR1(long t) { this.timerEndMsAsocijacijeR1 = t; }

    public long getTimerEndMsAsocijacijeR2() { return timerEndMsAsocijacijeR2; }
    public void setTimerEndMsAsocijacijeR2(long t) { this.timerEndMsAsocijacijeR2 = t; }
}