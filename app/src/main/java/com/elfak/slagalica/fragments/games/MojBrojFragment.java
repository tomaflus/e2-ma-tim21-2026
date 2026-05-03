package com.elfak.slagalica.fragments.games;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
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
    private boolean igraZavrsena = false;
    private int bodovi = 0;

    private CountDownTimer tajmerCiljni;
    private CountDownTimer tajmerGlavni;

    // Za detekciju shake-a
    private float zadnjaX, zadnjaY, zadnjaZ;
    private boolean prvaPromjena = true;
    private static final float SHAKE_LIMIT = 400;
    private static final int TRAJANJE_IGRE = 60000; // 60 sekundi
    private static final int TRAJANJE_STOP = 5000;  // 5 sekundi auto-otkrivanje

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
        pokrniTajmere();

        // Klik na Stop — otkrij ciljni broj
        binding.btnStopCiljni.setOnClickListener(v -> {
            if (!ciljniOtkriven) {
                otkrijCiljniBroj();
            }
        });

        // Klik na Stop — otkrij dostupne brojeve
        binding.btnStopBrojevi.setOnClickListener(v -> otkrijBrojeve());

        // Operandi klikovi
        binding.btnSaberi.setOnClickListener(v -> dodajUIzraz(" + "));
        binding.btnOduzmi.setOnClickListener(v -> dodajUIzraz(" - "));
        binding.btnPomnozi.setOnClickListener(v -> dodajUIzraz(" * "));
        binding.btnPodijeli.setOnClickListener(v -> dodajUIzraz(" / "));
        binding.btnZagraOtvori.setOnClickListener(v -> dodajUIzraz("("));
        binding.btnZagraZatvori.setOnClickListener(v -> dodajUIzraz(")"));

        // Brojevi klikovi
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

        binding.btnObrisi.setOnClickListener(v -> {
            String trenutni = binding.etIzraz.getText().toString();
            if (!trenutni.isEmpty()) {
                binding.etIzraz.setText(trenutni.substring(0, trenutni.length() - 1));
                binding.etIzraz.setSelection(binding.etIzraz.getText().length());
            }
        });

        // Potvrdi izraz
        binding.btnPotvrdi.setOnClickListener(v -> {
            if (igraZavrsena) return;
            String izraz = binding.etIzraz.getText().toString().trim();
            if (izraz.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite izraz!", Toast.LENGTH_SHORT).show();
                return;
            }
            evaluirajIzraz(izraz);
        });
    }

    private void pokrniTajmere() {
        // Tajmer 5s za auto-otkrivanje ciljnog broja
        tajmerCiljni = new CountDownTimer(TRAJANJE_STOP, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                otkrijCiljniBroj();
                // Tajmer 5s za auto-otkrivanje dostupnih brojeva
                new CountDownTimer(TRAJANJE_STOP, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {}

                    @Override
                    public void onFinish() {
                        otkrijBrojeve();
                    }
                }.start();
            }
        }.start();

        // Glavni tajmer 60s
        tajmerGlavni = new CountDownTimer(TRAJANJE_IGRE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTajmer.setText(millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                igraZavrsena = true;
                binding.btnPotvrdi.setEnabled(false);
                binding.tvTajmer.setText("0s");
                Toast.makeText(getContext(),
                        "Isteklo vrijeme! Bodovi: " + bodovi,
                        Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void otkrijCiljniBroj() {
        if (!ciljniOtkriven) {
            ciljniOtkriven = true;
            binding.tvCiljniBroj.setText(String.valueOf(ciljniBroj));
            binding.btnStopCiljni.setEnabled(false);
        }
    }

    private void otkrijBrojeve() {
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

    private void evaluirajIzraz(String izraz) {
        try {
            // Evaluacija matematičkog izraza
            double rezultat = evaluiraj(izraz);
            int rezultatInt = (int) rezultat;

            if (rezultatInt == ciljniBroj) {
                // Tačan rezultat!
                igraZavrsena = true;
                tajmerGlavni.cancel();
                bodovi = 10;
                azurirajBodove();
                Toast.makeText(getContext(),
                        "Tacno! Osvojili ste 10 bodova!",
                        Toast.LENGTH_LONG).show();
                binding.btnPotvrdi.setEnabled(false);
            } else {
                // Netačan rezultat
                Toast.makeText(getContext(),
                        "Rezultat je " + rezultatInt + ", a trazeni je " + ciljniBroj,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Neispravan izraz!", Toast.LENGTH_SHORT).show();
        }
    }

    // Evaluacija matematičkog izraza
    private double evaluiraj(String izraz) {
        izraz = izraz.trim();
        return evaluirajSabiranje(izraz, new int[]{0});
    }

    private double evaluirajSabiranje(String izraz, int[] pos) {
        double rezultat = evaluirajMnozenje(izraz, pos);
        while (pos[0] < izraz.length()) {
            char op = izraz.charAt(pos[0]);
            if (op != '+' && op != '-') break;
            pos[0]++;
            double desno = evaluirajMnozenje(izraz, pos);
            if (op == '+') rezultat += desno;
            else rezultat -= desno;
        }
        return rezultat;
    }

    private double evaluirajMnozenje(String izraz, int[] pos) {
        double rezultat = evaluirajBroj(izraz, pos);
        while (pos[0] < izraz.length()) {
            char op = izraz.charAt(pos[0]);
            if (op != '*' && op != '/') break;
            pos[0]++;
            double desno = evaluirajBroj(izraz, pos);
            if (op == '*') rezultat *= desno;
            else rezultat /= desno;
        }
        return rezultat;
    }

    private double evaluirajBroj(String izraz, int[] pos) {
        // Preskoči razmake
        while (pos[0] < izraz.length() && izraz.charAt(pos[0]) == ' ') pos[0]++;

        if (pos[0] < izraz.length() && izraz.charAt(pos[0]) == '(') {
            pos[0]++; // preskoči '('
            double rezultat = evaluirajSabiranje(izraz, pos);
            pos[0]++; // preskoči ')'
            return rezultat;
        }

        int pocetak = pos[0];
        if (pos[0] < izraz.length() && izraz.charAt(pos[0]) == '-') pos[0]++;
        while (pos[0] < izraz.length() &&
                (Character.isDigit(izraz.charAt(pos[0])) || izraz.charAt(pos[0]) == '.')) {
            pos[0]++;
        }

        // Preskoči razmake
        while (pos[0] < izraz.length() && izraz.charAt(pos[0]) == ' ') pos[0]++;

        return Double.parseDouble(izraz.substring(pocetak, pos[0]).trim());
    }

    private void azurirajBodove() {
        binding.tvBodovi.setText("Bodovi: " + bodovi);
    }

    private void generisiBrojeve() {
        Random random = new Random();
        ciljniBroj = random.nextInt(900) + 100;

        for (int i = 0; i < 4; i++) {
            dostupniBrojevi[i] = random.nextInt(9) + 1;
        }
        int[] srednji = {10, 15, 20};
        dostupniBrojevi[4] = srednji[random.nextInt(3)];

        int[] veliki = {25, 50, 75, 100};
        dostupniBrojevi[5] = veliki[random.nextInt(4)];
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
                    otkrijBrojeve();
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
        if (tajmerGlavni != null) tajmerGlavni.cancel();
        if (tajmerCiljni != null) tajmerCiljni.cancel();
        binding = null;
    }


}