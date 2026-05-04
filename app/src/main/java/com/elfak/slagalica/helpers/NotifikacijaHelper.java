package com.elfak.slagalica.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotifikacijaHelper {

    private static final String KANAL_CET = "kanal_cet";
    private static int notifikacijaId = 0;

    // Inicijalizuj notifikacijski kanal
    public static void kreirajKanale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kanal = new NotificationChannel(
                    KANAL_CET,
                    "Chat notifikacije",
                    NotificationManager.IMPORTANCE_HIGH
            );
            kanal.setDescription("Notifikacije za nove poruke u chatu");

            NotificationManager manager = context.getSystemService(
                    NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(kanal);
            }
        }
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