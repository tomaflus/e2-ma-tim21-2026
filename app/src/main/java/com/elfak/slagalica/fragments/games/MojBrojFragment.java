package com.elfak.slagalica.fragments.games;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentMojBrojBinding;

import java.util.Random;

public class MojBrojFragment extends Fragment implements SensorEventListener {

    private FragmentMojBrojBinding binding;
    private SensorManager sensorManager;
    private Sensor akcelerometar;

    private int ciljniBroj = 0;
    private int[] dostupniBrojevi = new int[6];
    private boolean ciljniOtkriven = false;
    private boolean brojeviOtkriveni = false;

    // Za detekciju shake-a
    private float zadnjaX, zadnjaY, zadnjaZ;
    private boolean prvaPromjena = true;
    private static final float SHAKE_LIMIT = 800;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMojBrojBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicijalizacija senzora
        sensorManager = (SensorManager) requireActivity()
                .getSystemService(android.content.Context.SENSOR_SERVICE);
        akcelerometar = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        generisiBrojeve();

        // Klik na Stop — otkrij ciljni broj
        binding.btnStopCiljni.setOnClickListener(v -> {
            if (!ciljniOtkriven) {
                ciljniOtkriven = true;
                binding.tvCiljniBroj.setText(String.valueOf(ciljniBroj));
                binding.btnStopCiljni.setEnabled(false);
            }
        });

        // Klik na Stop — otkrij dostupne brojeve
        binding.btnStopBrojevi.setOnClickListener(v -> otkrij());

        // Operandi klikovi — dodaju u izraz
        binding.btnSaberi.setOnClickListener(v -> dodajUIzraz(" + "));
        binding.btnOduzmi.setOnClickListener(v -> dodajUIzraz(" - "));
        binding.btnPomnozi.setOnClickListener(v -> dodajUIzraz(" * "));
        binding.btnPodijeli.setOnClickListener(v -> dodajUIzraz(" / "));
        binding.btnZagraOtvori.setOnClickListener(v -> dodajUIzraz("("));
        binding.btnZagraZatvori.setOnClickListener(v -> dodajUIzraz(")"));

        // Brojevi klikovi — dodaju u izraz
        binding.btnBroj1.setOnClickListener(v -> {
            if (binding.btnBroj1.getText() != null)
                dodajUIzraz(binding.btnBroj1.getText().toString());
        });
        binding.btnBroj2.setOnClickListener(v -> {
            if (binding.btnBroj2.getText() != null)
                dodajUIzraz(binding.btnBroj2.getText().toString());
        });
        binding.btnBroj3.setOnClickListener(v -> {
            if (binding.btnBroj3.getText() != null)
                dodajUIzraz(binding.btnBroj3.getText().toString());
        });
        binding.btnBroj4.setOnClickListener(v -> {
            if (binding.btnBroj4.getText() != null)
                dodajUIzraz(binding.btnBroj4.getText().toString());
        });
        binding.btnBroj5.setOnClickListener(v -> {
            if (binding.btnBroj5.getText() != null)
                dodajUIzraz(binding.btnBroj5.getText().toString());
        });
        binding.btnBroj6.setOnClickListener(v -> {
            if (binding.btnBroj6.getText() != null)
                dodajUIzraz(binding.btnBroj6.getText().toString());
        });

        // Potvrdi izraz
        binding.btnPotvrdi.setOnClickListener(v -> {
            String izraz = binding.etIzraz.getText().toString().trim();
            if (izraz.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite izraz!", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO: Evaluacija izraza i provjera dolazi u KT2
            Toast.makeText(getContext(),
                    "Izraz: " + izraz, Toast.LENGTH_SHORT).show();
        });
    }

    private void generisiBrojeve() {
        Random random = new Random();

        // Ciljni broj (100-999)
        ciljniBroj = random.nextInt(900) + 100;

        // 4 jednocifrena broja
        for (int i = 0; i < 4; i++) {
            dostupniBrojevi[i] = random.nextInt(9) + 1;
        }
        // Jedan broj: 10, 15 ili 20
        int[] srednji = {10, 15, 20};
        dostupniBrojevi[4] = srednji[random.nextInt(3)];

        // Jedan broj: 25, 50, 75 ili 100
        int[] veliki = {25, 50, 75, 100};
        dostupniBrojevi[5] = veliki[random.nextInt(4)];
    }

    private void otkrij() {
        if (!brojeviOtkriveni) {
            brojeviOtkriveni = true;
            binding.btnBroj1.setText(String.valueOf(dostupniBrojevi[0]));
            binding.btnBroj2.setText(String.valueOf(dostupniBrojevi[1]));
            binding.btnBroj3.setText(String.valueOf(dostupniBrojevi[2]));
            binding.btnBroj4.setText(String.valueOf(dostupniBrojevi[3]));
            binding.btnBroj5.setText(String.valueOf(dostupniBrojevi[4]));
            binding.btnBroj6.setText(String.valueOf(dostupniBrojevi[5]));
            binding.btnStopBrojevi.setEnabled(false);
        }
    }

    private void dodajUIzraz(String vrijednost) {
        String trenutni = binding.etIzraz.getText().toString();
        binding.etIzraz.setText(trenutni + vrijednost);
        binding.etIzraz.setSelection(binding.etIzraz.getText().length());
    }

    // Shake senzor
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (!prvaPromjena) {
                float razlika = Math.abs(x - zadnjaX)
                        + Math.abs(y - zadnjaY)
                        + Math.abs(z - zadnjaZ);

                if (razlika > SHAKE_LIMIT) {
                    otkrij();
                    Toast.makeText(getContext(),
                            "Shake detektovan!", Toast.LENGTH_SHORT).show();
                }
            }

            zadnjaX = x;
            zadnjaY = y;
            zadnjaZ = z;
            prvaPromjena = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (akcelerometar != null) {
            sensorManager.registerListener(this, akcelerometar,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}