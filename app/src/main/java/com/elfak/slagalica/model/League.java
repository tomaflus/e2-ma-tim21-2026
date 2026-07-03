package com.elfak.slagalica.model;

import com.elfak.slagalica.R;

public enum League {
    POCETNIK("Početnici", 0, 0, R.drawable.ic_league_bronze),
    NOVI_ASOVI("Novi asovi", 100, 1, R.drawable.ic_league_bronze),
    BRONZANI_RATNICI("Bronzani ratnici", 200, 2, R.drawable.ic_league_silver),
    SREBRNI_SAMPIONI("Srebrni šampioni", 400, 3, R.drawable.ic_league_gold),
    PLATINASTI_VITEZOVI("Platinasti vitezovi", 800, 4, R.drawable.ic_league_platinum),
    ZLATNE_LEGENDE("Zlatne legende", 1600, 5, R.drawable.ic_league_diamond);

    private final String name;
    private final int minStars;
    private final int bonusTokens;
    private final int iconRes;

    League(String name, int minStars, int bonusTokens, int iconRes) {
        this.name = name;
        this.minStars = minStars;
        this.bonusTokens = bonusTokens;
        this.iconRes = iconRes;
    }

    public String getName() { return name; }
    public int getMinStars() { return minStars; }
    public int getBonusTokens() { return bonusTokens; }
    public int getIconRes() { return iconRes; }

    public static League getByStars(int stars) {
        if (stars >= 1600) return ZLATNE_LEGENDE;
        if (stars >= 800) return PLATINASTI_VITEZOVI;
        if (stars >= 400) return SREBRNI_SAMPIONI;
        if (stars >= 200) return BRONZANI_RATNICI;
        if (stars >= 100) return NOVI_ASOVI;
        return POCETNIK;
    }
}