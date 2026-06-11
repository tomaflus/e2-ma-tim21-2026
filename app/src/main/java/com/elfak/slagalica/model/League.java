package com.elfak.slagalica.model;

import com.elfak.slagalica.R;

public enum League {
    PUN_POCETNIK("Početnik", 0, 0, R.drawable.ic_league_bronze),
    BRONZANA("Bronzana", 100, 1, R.drawable.ic_league_bronze),
    SREBRNA("Srebrna", 200, 2, R.drawable.ic_league_silver),
    ZLATNA("Zlatna", 400, 3, R.drawable.ic_league_gold),
    PLATINASTA("Platinasta", 800, 4, R.drawable.ic_league_platinum),
    DIJAMANTSKA("Dijamantska", 1600, 5, R.drawable.ic_league_diamond);

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
        if (stars >= 1600) return DIJAMANTSKA;
        if (stars >= 800) return PLATINASTA;
        if (stars >= 400) return ZLATNA;
        if (stars >= 200) return SREBRNA;
        if (stars >= 100) return BRONZANA;
        return PUN_POCETNIK;
    }
}