package com.elfak.slagalica.activities;

import android.content.pm.PackageManager;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.elfak.slagalica.R;
import com.elfak.slagalica.helpers.NotifikacijaHelper;

public class MainActivity extends AppCompatActivity {

    private static final int KOD_DOZVOLE_NOTIFIKACIJA = 100;

    public static final String KANAL_CET = "kanal_cet";
    public static final String KANAL_RANGIRANJE = "kanal_rangiranje";
    public static final String KANAL_NAGRADE = "kanal_nagrade";
    public static final String KANAL_OSTALO = "kanal_ostalo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicijalizuj notifikacijske kanale
        NotifikacijaHelper.kreirajKanale(this);

        // Traži dozvolu za notifikacije na Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        KOD_DOZVOLE_NOTIFIKACIJA);
            }
        }
        kreirajKanaleNotifikacija();
    }

    private void kreirajKanaleNotifikacija() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager mgr = getSystemService(NotificationManager.class);
        kreirajKanal(mgr, KANAL_CET, "Čet", "Obavještenja o novim porukama u četu", NotificationManager.IMPORTANCE_DEFAULT);
        kreirajKanal(mgr, KANAL_RANGIRANJE, "Rang lista", "Obavještenja o plasmanu na rang listi", NotificationManager.IMPORTANCE_DEFAULT);
        kreirajKanal(mgr, KANAL_NAGRADE, "Nagrade", "Obavještenja o nagradama i prelasku u ligu", NotificationManager.IMPORTANCE_HIGH);
        kreirajKanal(mgr, KANAL_OSTALO, "Ostalo", "Ostala sistemska obavještenja", NotificationManager.IMPORTANCE_LOW);
    }

    private void kreirajKanal(NotificationManager mgr, String id, String naziv, String opis, int vaznost) {
        NotificationChannel kanal = new NotificationChannel(id, naziv, vaznost);
        kanal.setDescription(opis);
        mgr.createNotificationChannel(kanal);
    }
}