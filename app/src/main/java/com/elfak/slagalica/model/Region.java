package com.elfak.slagalica.model;

import com.elfak.slagalica.R;

public enum Region {
    VOJVODINA("Vojvodina", "Vojvodina", 45.45, 19.8, R.drawable.ic_region_vojvodina, 0x504CAF50),
    BEOGRAD("Beograd", "Beograd", 44.78, 20.45, R.drawable.ic_region_beograd, 0x502196F3),
    SUMADIJA("Šumadija i Zapadna", "Šumadija i Zapadna Srbija", 43.9, 19.8, R.drawable.ic_region_sumadija, 0x509C27B0),
    ISTOCNA("Južna i Istočna", "Južna i Istočna Srbija", 43.3, 21.8, R.drawable.ic_region_istok, 0x50FFC107),
    KOSOVO("Kosovo i Metohija", "Kosovo i Metohija", 42.5, 20.8, R.drawable.ic_region_kosovo, 0x50F44336);

    private final String shortName;
    private final String fullName;
    private final double centerLat;
    private final double centerLon;
    private final int iconRes;
    private final int color;

    Region(String shortName, String fullName, double lat, double lon, int iconRes, int color) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.centerLat = lat;
        this.centerLon = lon;
        this.iconRes = iconRes;
        this.color = color;
    }

    public String getShortName() { return shortName; }
    public String getFullName() { return fullName; }
    public double getCenterLat() { return centerLat; }
    public double getCenterLon() { return centerLon; }
    public int getIconRes() { return iconRes; }
    public int getColor() { return color; }

    public static Region getByName(String name) {
        if (name == null) return BEOGRAD;
        for (Region r : Region.values()) {
            if (r.fullName.equalsIgnoreCase(name) || r.name().equalsIgnoreCase(name) || r.shortName.equalsIgnoreCase(name)) return r;
        }
        return BEOGRAD;
    }
}
