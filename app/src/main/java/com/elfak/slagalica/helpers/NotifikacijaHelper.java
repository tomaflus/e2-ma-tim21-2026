package com.elfak.slagalica.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotifikacijaHelper {

    public static final String KANAL_CET       = "kanal_cet";
    public static final String KANAL_RANGIRANJE = "kanal_rangiranje";
    public static final String KANAL_NAGRADE   = "kanal_nagrade";
    public static final String KANAL_OSTALO    = "kanal_ostalo";

    private static int notifikacijaId = 0;

    public static void kreirajKanale(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        if (mgr == null) return;
        kreirajKanal(mgr, KANAL_CET,        "Čet",       "Obavještenja o novim porukama u četu",        NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_RANGIRANJE,  "Rang lista","Obavještenja o plasmanu na rang listi",        NotificationManager.IMPORTANCE_DEFAULT);
        kreirajKanal(mgr, KANAL_NAGRADE,    "Nagrade",   "Obavještenja o nagradama i prelasku u ligu",   NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_OSTALO,     "Ostalo",    "Ostala sistemska obavještenja",                NotificationManager.IMPORTANCE_LOW);
    }

    private static void kreirajKanal(NotificationManager mgr, String id, String naziv, String opis, int vaznost) {
        NotificationChannel kanal = new NotificationChannel(id, naziv, vaznost);
        kanal.setDescription(opis);
        mgr.createNotificationChannel(kanal);
    }

    // Prikaži notifikaciju za novu poruku
    public static void prikaziNotifikacijuCet(Context context,
                                              String posiljac, String poruka) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, KANAL_CET)
                        .setSmallIcon(android.R.drawable.ic_dialog_email)
                        .setContentTitle("Nova poruka od " + posiljac)
                        .setContentText(poruka)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifikacijaId++, builder.build());
        }
    }
}