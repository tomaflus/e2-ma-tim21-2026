package com.elfak.slagalica.model;

public enum Region {
    VOJVODINA("Vojvodina", "Vojvodina"),
    BEOGRAD("Beograd", "Beograd"),
    SUMADIJA("Šumadija", "Šumadija i zapadna Srbija"),
    ISTOCNA("Istok", "Južna i istočna Srbija"),
    KOSOVO("Kosovo", "Kosovo i Metohija");

    private final String shortName;
    private final String fullName;

    Region(String shortName, String fullName) {
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public String getShortName() { return shortName; }
    public String getFullName() { return fullName; }

    public static Region getByName(String name) {
        for (Region r : Region.values()) {
            if (r.fullName.equalsIgnoreCase(name) || r.name().equalsIgnoreCase(name)) return r;
        }
        return BEOGRAD;
    }
}