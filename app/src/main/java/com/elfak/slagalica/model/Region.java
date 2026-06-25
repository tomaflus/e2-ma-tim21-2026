package com.elfak.slagalica.model;

import com.elfak.slagalica.R;

public enum Region {
    VOJVODINA("Vojvodina", "Vojvodina", 45.2609, 19.8317, 1.0, R.drawable.map_serbia_regions),
    BEOGRAD("Beograd", "Beograd", 44.7866, 20.4489, 0.3, R.drawable.map_serbia_regions),
    SUMADIJA("Šumadija", "Šumadija i zapadna Srbija", 43.9373, 20.3703, 0.8, R.drawable.map_serbia_regions),
    ISTOCNA("Istok", "Južna i istočna Srbija", 43.3209, 21.8958, 0.8, R.drawable.map_serbia_regions),
    KOSOVO("Kosovo", "Kosovo i Metohija", 42.6629, 21.1655, 0.5, R.drawable.map_serbia_regions);

    private final String shortName;
    private final String fullName;
    private final double centerLat;
    private final double centerLon;
    private final double radius; // rough radius for random points
    private final int iconRes;

    Region(String shortName, String fullName, double lat, double lon, double radius, int iconRes) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.centerLat = lat;
        this.centerLon = lon;
        this.radius = radius;
        this.iconRes = iconRes;
    }

    public String getShortName() { return shortName; }
    public String getFullName() { return fullName; }
    public double getCenterLat() { return centerLat; }
    public double getCenterLon() { return centerLon; }
    public double getRadius() { return radius; }
    public int getIconRes() { return iconRes; }

    public static Region getByName(String name) {
        if (name == null) return BEOGRAD;
        for (Region r : Region.values()) {
            if (r.fullName.equalsIgnoreCase(name) || r.name().equalsIgnoreCase(name) || r.shortName.equalsIgnoreCase(name)) return r;
        }
        return BEOGRAD;
    }
}