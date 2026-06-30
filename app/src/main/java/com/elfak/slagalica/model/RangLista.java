package com.elfak.slagalica.model;

public class RangLista {
    private String id; // postavlja se lokalno, nije u Firestoreu
    private String tip; // "nedeljna" | "mesecna"
    private String ciklusId;
    private String opseg; // npr. "22.06 - 28.06.2026." ili "Jun 2026."
    private boolean aktivna;

    public RangLista() {}

    public RangLista(String tip, String ciklusId, String opseg, boolean aktivna) {
        this.tip = tip;
        this.ciklusId = ciklusId;
        this.opseg = opseg;
        this.aktivna = aktivna;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
    public String getCiklusId() { return ciklusId; }
    public void setCiklusId(String ciklusId) { this.ciklusId = ciklusId; }
    public String getOpseg() { return opseg; }
    public void setOpseg(String opseg) { this.opseg = opseg; }
    public boolean isAktivna() { return aktivna; }
    public void setAktivna(boolean aktivna) { this.aktivna = aktivna; }
}
