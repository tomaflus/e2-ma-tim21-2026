package com.elfak.slagalica.model;

public class DnevneMisije {
    private String danId = "";
    private boolean pobedaUradjeno;
    private boolean porukaCetUradjeno;
    private boolean prijateljskaUradjeno;
    private boolean turnirPobedaUradjeno;
    private boolean bonusDodeljen;

    public DnevneMisije() {}

    public String getDanId() { return danId; }
    public void setDanId(String danId) { this.danId = danId; }
    public boolean isPobedaUradjeno() { return pobedaUradjeno; }
    public void setPobedaUradjeno(boolean v) { this.pobedaUradjeno = v; }
    public boolean isPorukaCetUradjeno() { return porukaCetUradjeno; }
    public void setPorukaCetUradjeno(boolean v) { this.porukaCetUradjeno = v; }
    public boolean isPrijateljskaUradjeno() { return prijateljskaUradjeno; }
    public void setPrijateljskaUradjeno(boolean v) { this.prijateljskaUradjeno = v; }
    public boolean isTurnirPobedaUradjeno() { return turnirPobedaUradjeno; }
    public void setTurnirPobedaUradjeno(boolean v) { this.turnirPobedaUradjeno = v; }
    public boolean isBonusDodeljen() { return bonusDodeljen; }
    public void setBonusDodeljen(boolean v) { this.bonusDodeljen = v; }
}
