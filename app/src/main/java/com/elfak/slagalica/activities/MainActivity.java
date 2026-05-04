package com.elfak.slagalica.activities;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.elfak.slagalica.R;
import com.elfak.slagalica.helpers.NotifikacijaHelper;

public class MainActivity extends AppCompatActivity {

    private static final int KOD_DOZVOLE_NOTIFIKACIJA = 100;

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
    }
}