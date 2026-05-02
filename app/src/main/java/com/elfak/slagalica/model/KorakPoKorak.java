package com.elfak.slagalica.model;

public class KorakPoKorak {
    private String id;
    private String rjesenje;
    private String korak1;
    private String korak2;
    private String korak3;
    private String korak4;
    private String korak5;
    private String korak6;
    private String korak7;

    public KorakPoKorak() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRjesenje() { return rjesenje; }
    public void setRjesenje(String rjesenje) { this.rjesenje = rjesenje; }

    public String getKorak1() { return korak1; }
    public void setKorak1(String korak1) { this.korak1 = korak1; }

    public String getKorak2() { return korak2; }
    public void setKorak2(String korak2) { this.korak2 = korak2; }

    public String getKorak3() { return korak3; }
    public void setKorak3(String korak3) { this.korak3 = korak3; }

    public String getKorak4() { return korak4; }
    public void setKorak4(String korak4) { this.korak4 = korak4; }

    public String getKorak5() { return korak5; }
    public void setKorak5(String korak5) { this.korak5 = korak5; }

    public String getKorak6() { return korak6; }
    public void setKorak6(String korak6) { this.korak6 = korak6; }

    public String getKorak7() { return korak7; }
    public void setKorak7(String korak7) { this.korak7 = korak7; }

    public String[] getKoraci() {
        return new String[]{korak1, korak2, korak3, korak4, korak5, korak6, korak7};
    }
}