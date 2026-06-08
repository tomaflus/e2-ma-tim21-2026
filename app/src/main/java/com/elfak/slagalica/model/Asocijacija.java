package com.elfak.slagalica.model;

public class Asocijacija {
    private String id;
    private String poljeA1, poljeA2, poljeA3, poljeA4;
    private String poljeB1, poljeB2, poljeB3, poljeB4;
    private String poljeC1, poljeC2, poljeC3, poljeC4;
    private String poljeD1, poljeD2, poljeD3, poljeD4;
    private String rjesenjeA, rjesenjeB, rjesenjeC, rjesenjeD;
    private String konacnoRjesenje;

    public Asocijacija() {}

    public String[][] getPolja() {
        return new String[][]{
            {poljeA1, poljeA2, poljeA3, poljeA4},
            {poljeB1, poljeB2, poljeB3, poljeB4},
            {poljeC1, poljeC2, poljeC3, poljeC4},
            {poljeD1, poljeD2, poljeD3, poljeD4}
        };
    }

    public String[] getRjesenja() {
        return new String[]{rjesenjeA, rjesenjeB, rjesenjeC, rjesenjeD};
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPoljeA1() { return poljeA1; }
    public void setPoljeA1(String v) { this.poljeA1 = v; }
    public String getPoljeA2() { return poljeA2; }
    public void setPoljeA2(String v) { this.poljeA2 = v; }
    public String getPoljeA3() { return poljeA3; }
    public void setPoljeA3(String v) { this.poljeA3 = v; }
    public String getPoljeA4() { return poljeA4; }
    public void setPoljeA4(String v) { this.poljeA4 = v; }

    public String getPoljeB1() { return poljeB1; }
    public void setPoljeB1(String v) { this.poljeB1 = v; }
    public String getPoljeB2() { return poljeB2; }
    public void setPoljeB2(String v) { this.poljeB2 = v; }
    public String getPoljeB3() { return poljeB3; }
    public void setPoljeB3(String v) { this.poljeB3 = v; }
    public String getPoljeB4() { return poljeB4; }
    public void setPoljeB4(String v) { this.poljeB4 = v; }

    public String getPoljeC1() { return poljeC1; }
    public void setPoljeC1(String v) { this.poljeC1 = v; }
    public String getPoljeC2() { return poljeC2; }
    public void setPoljeC2(String v) { this.poljeC2 = v; }
    public String getPoljeC3() { return poljeC3; }
    public void setPoljeC3(String v) { this.poljeC3 = v; }
    public String getPoljeC4() { return poljeC4; }
    public void setPoljeC4(String v) { this.poljeC4 = v; }

    public String getPoljeD1() { return poljeD1; }
    public void setPoljeD1(String v) { this.poljeD1 = v; }
    public String getPoljeD2() { return poljeD2; }
    public void setPoljeD2(String v) { this.poljeD2 = v; }
    public String getPoljeD3() { return poljeD3; }
    public void setPoljeD3(String v) { this.poljeD3 = v; }
    public String getPoljeD4() { return poljeD4; }
    public void setPoljeD4(String v) { this.poljeD4 = v; }

    public String getRjesenjeA() { return rjesenjeA; }
    public void setRjesenjeA(String v) { this.rjesenjeA = v; }
    public String getRjesenjeB() { return rjesenjeB; }
    public void setRjesenjeB(String v) { this.rjesenjeB = v; }
    public String getRjesenjeC() { return rjesenjeC; }
    public void setRjesenjeC(String v) { this.rjesenjeC = v; }
    public String getRjesenjeD() { return rjesenjeD; }
    public void setRjesenjeD(String v) { this.rjesenjeD = v; }

    public String getKonacnoRjesenje() { return konacnoRjesenje; }
    public void setKonacnoRjesenje(String v) { this.konacnoRjesenje = v; }
}
