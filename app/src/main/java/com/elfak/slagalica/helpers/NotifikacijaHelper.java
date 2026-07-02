package com.elfak.slagalica.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.elfak.slagalica.R;
import com.elfak.slagalica.activities.MainActivity;

import androidx.core.app.NotificationCompat;

public class NotifikacijaHelper {

    public static final String KANAL_CET       = "kanal_cet";
    public static final String KANAL_RANGIRANJE = "kanal_rangiranje";
    public static final String KANAL_NAGRADE   = "kanal_nagrade";
    public static final String KANAL_MISIJE    = "kanal_misije";
    public static final String KANAL_OSTALO    = "kanal_ostalo";

    private static int notifikacijaId = 0;

    public static void kreirajKanale(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        if (mgr == null) return;
        kreirajKanal(mgr, KANAL_CET,        "Čet",       "Obavještenja o novim porukama u četu",        NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_RANGIRANJE,  "Rang lista","Obavještenja o plasmanu na rang listi",        NotificationManager.IMPORTANCE_DEFAULT);
        kreirajKanal(mgr, KANAL_NAGRADE,    "Nagrade",   "Obavještenja o nagradama i prelasku u ligu",   NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_MISIJE,     "Dnevne misije", "Obavještenja o ispunjenim dnevnim misijama", NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_OSTALO,     "Ostalo",    "Ostala sistemska obavještenja",                NotificationManager.IMPORTANCE_LOW);
    }

    private static void kreirajKanal(NotificationManager mgr, String id, String naziv, String opis, int vaznost) {
        NotificationChannel kanal = new NotificationChannel(id, naziv, vaznost);
        kanal.setDescription(opis);
        mgr.createNotificationChannel(kanal);
    }

    public static void prikaziNotifikacijuPoziv(Context context, String posiljac) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, KANAL_OSTALO)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Poziv na partiju")
                        .setContentText(posiljac + " te poziva na prijateljsku partiju!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifikacijaId++, builder.build());
        }
    }

    public static void prikaziNotifikacijuNagradu(Context context, int tokeni) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, KANAL_NAGRADE)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Nagrade za rang listu!")
                        .setContentText("Osvoji ste " + tokeni + " tokena za plasman na rang listi.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifikacijaId++, builder.build());
        }
    }

    public static void prikaziNotifikacijuMisiju(Context context, String naziv, int zvezde, boolean sveMisije) {
        String naslov;
        String tekst;
        if (sveMisije) {
            naslov = "Sve misije ispunjene!";
            tekst = naziv + " — Bonus: +" + zvezde + " ★ i +2 tokena!";
        } else {
            naslov = "Misija ispunjena!";
            tekst = naziv + " — +" + zvezde + " ★";
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("navigateTo", "dnevneMisije");
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, KANAL_MISIJE)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(naslov)
                        .setContentText(tekst)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(notifikacijaId++, builder.build());
    }

    // Prikaži notifikaciju za novu poruku
    public static void prikaziNotifikacijuCet(Context context,
                                              String posiljac, String poruka) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, KANAL_CET)
                        .setSmallIcon(R.mipmap.ic_launcher)
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