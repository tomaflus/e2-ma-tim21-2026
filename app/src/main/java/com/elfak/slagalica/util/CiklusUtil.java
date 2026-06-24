package com.elfak.slagalica.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CiklusUtil {

    public static String trenutniDanId() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static String trenutniNedeljaId() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        return String.format(Locale.getDefault(), "%d-W%02d", year, week);
    }

    public static String trenutniMesecId() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    public static String opsegNedelje() {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String pocetak = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(c.getTime());
        c.add(Calendar.DAY_OF_MONTH, 6);
        String kraj = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()).format(c.getTime());
        return pocetak + " - " + kraj;
    }

    public static String opsegMeseca() {
        String[] meseci = {
            "Januar", "Februar", "Mart", "April", "Maj", "Jun",
            "Jul", "Avgust", "Septembar", "Oktobar", "Novembar", "Decembar"
        };
        Calendar c = Calendar.getInstance();
        return meseci[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR) + ".";
    }
}
