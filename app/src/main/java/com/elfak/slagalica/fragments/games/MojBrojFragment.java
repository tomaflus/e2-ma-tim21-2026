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
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentMojBrojBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Stack za pracenje tokena i korištenih dugmadi
    private Stack<String> tokeniIzraza = new Stack<>();
    private Stack<Button> koristenaDugmad = new Stack<>();

    private float zadnjaX, zadnjaY, zadnjaZ;
    private boolean prvaPromjena = true;
    private static final float SHAKE_LIMIT = 400;
    private static final int TRAJANJE_IGRE = 60000;
    private static final int TRAJANJE_STOP = 5000;

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

        sensorManager = (SensorManager) requireActivity()
                .getSystemService(android.content.Context.SENSOR_SERVICE);
        akcelerometar = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        generisiBrojeve();
        pokrniTajmere();

        binding.btnStopCiljni.setOnClickListener(v -> otkrijCiljniBroj());
        binding.btnStopBrojevi.setOnClickListener(v -> otkrijBrojeve());

        // Operandi — dodaju token bez dugmeta
        binding.btnSaberi.setOnClickListener(v -> dodajToken(" + ", null));
        binding.btnOduzmi.setOnClickListener(v -> dodajToken(" - ", null));
        binding.btnPomnozi.setOnClickListener(v -> dodajToken(" * ", null));
        binding.btnPodijeli.setOnClickListener(v -> dodajToken(" / ", null));
        binding.btnZagraOtvori.setOnClickListener(v -> dodajToken("(", null));
        binding.btnZagraZatvori.setOnClickListener(v -> dodajToken(")", null));

        // Brojevi — dodaju token sa dugmetom
        binding.btnBroj1.setOnClickListener(v -> klikniNaBroj(binding.btnBroj1));
        binding.btnBroj2.setOnClickListener(v -> klikniNaBroj(binding.btnBroj2));
        binding.btnBroj3.setOnClickListener(v -> klikniNaBroj(binding.btnBroj3));
        binding.btnBroj4.setOnClickListener(v -> klikniNaBroj(binding.btnBroj4));
        binding.btnBroj5.setOnClickListener(v -> klikniNaBroj(binding.btnBroj5));
        binding.btnBroj6.setOnClickListener(v -> klikniNaBroj(binding.btnBroj6));

        // Obrisi cijeli zadnji token
        binding.btnObrisi.setOnClickListener(v -> {
            if (tokeniIzraza.isEmpty()) return;

            String zadnjiToken = tokeniIzraza.pop();
            Button dugme = koristenaDugmad.pop();

            // Vrati dugme ako je broj
            if (dugme != null) {
                dugme.setEnabled(true);
                dugme.setAlpha(1.0f);
            }

            // Obriši token iz izraza
            String trenutni = binding.etIzraz.getText().toString();
            if (trenutni.endsWith(zadnjiToken)) {
                binding.etIzraz.setText(
                        trenutni.substring(0, trenutni.length() - zadnjiToken.length()));
                binding.etIzraz.setSelection(binding.etIzraz.getText().length());
            }
        });

        binding.btnPotvrdi.setOnClickListener(v -> {
            if (igraZavrsena) return;
            String izraz = binding.etIzraz.getText().toString().trim();
            if (izraz.isEmpty()) {
                Toast.makeText(getContext(), "Unesite izraz!", Toast.LENGTH_SHORT).show();
                return;
            }
            evaluirajIzraz(izraz);
        });
    }

    private void dodajToken(String token, Button dugme) {
        // Dodaj token u izraz
        String trenutni = binding.etIzraz.getText().toString();
        binding.etIzraz.setText(trenutni + token);
        binding.etIzraz.setSelection(binding.etIzraz.getText().length());

        // Sačuvaj token i dugme u stack
        tokeniIzraza.push(token);
        koristenaDugmad.push(dugme);
    }

    private void klikniNaBroj(Button dugme) {
        if (!brojeviOtkriveni) {
            Toast.makeText(getContext(),
                    "Prvo otkrijte brojeve!", Toast.LENGTH_SHORT).show();
            return;
        }

        String broj = dugme.getText().toString();
        dodajToken(broj, dugme);

        // Onemogući dugme
        dugme.setEnabled(false);
        dugme.setAlpha(0.4f);
    }

    private void pokrniTajmere() {
        tajmerCiljni = new CountDownTimer(TRAJANJE_STOP, 1000) {
            @Override public void onTick(long ms) {}
            @Override public void onFinish() {
                otkrijCiljniBroj();
                new CountDownTimer(TRAJANJE_STOP, 1000) {
                    @Override public void onTick(long ms) {}
                    @Override public void onFinish() { otkrijBrojeve(); }
                }.start();
            }
        }.start();

        tajmerGlavni = new CountDownTimer(TRAJANJE_IGRE, 1000) {
            @Override
            public void onTick(long ms) {
                binding.tvTajmer.setText(ms / 1000 + "s");
            }
            @Override
            public void onFinish() {
                igraZavrsena = true;
                binding.btnPotvrdi.setEnabled(false);
                binding.tvTajmer.setText("0s");
                Toast.makeText(getContext(),
                        "Isteklo vrijeme! Bodovi: " + bodovi, Toast.LENGTH_LONG).show();
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
        if (!brojeviOtkriveni) {
            Toast.makeText(getContext(),
                    "Prvo otkrijte dostupne brojeve!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double rezultat = new ExprParser(izraz.replaceAll("\\s+", "")).parse();
            int rezultatInt = (int) rezultat;

            if (rezultatInt == ciljniBroj) {
                igraZavrsena = true;
                tajmerGlavni.cancel();
                bodovi = 10;
                azurirajBodove();
                binding.btnPotvrdi.setEnabled(false);
                Toast.makeText(getContext(),
                        "Tacno! Osvojili ste 10 bodova!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(),
                        "Rezultat je " + rezultatInt + ", trazeni je " + ciljniBroj,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Neispravan izraz!", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ExprParser {
        private final String expr;
        private int pos = 0;

        ExprParser(String expr) { this.expr = expr; }

        double parse() throws Exception {
            double result = parseExpr();
            if (pos != expr.length()) throw new Exception("Neispravan izraz");
            return result;
        }

        private double parseExpr() throws Exception {
            double result = parseTerm();
            while (pos < expr.length() &&
                    (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) {
                char op = expr.charAt(pos++);
                double term = parseTerm();
                result = (op == '+') ? result + term : result - term;
            }
            return result;
        }

        private double parseTerm() throws Exception {
            double result = parseFactor();
            while (pos < expr.length() &&
                    (expr.charAt(pos) == '*' || expr.charAt(pos) == '/')) {
                char op = expr.charAt(pos++);
                double factor = parseFactor();
                if (op == '/' && factor == 0) throw new Exception("Dijeljenje s nulom");
                result = (op == '*') ? result * factor : result / factor;
            }
            return result;
        }

        private double parseFactor() throws Exception {
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++;
                double result = parseExpr();
                if (pos >= expr.length() || expr.charAt(pos) != ')')
                    throw new Exception("Nedostaje zatvorena zagrada");
                pos++;
                return result;
            }

            boolean negative = false;
            if (pos < expr.length() && expr.charAt(pos) == '-') {
                negative = true;
                pos++;
            }

            int start = pos;
            while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) pos++;
            if (pos == start) throw new Exception("Ocekivan broj na poziciji " + pos);

            double value = Double.parseDouble(expr.substring(start, pos));
            return negative ? -value : value;
        }
    }

    private void azurirajBodove() {
        binding.tvBodovi.setText("Bodovi: " + bodovi);
    }

    private void generisiBrojeve() {
        Random random = new Random();
        ciljniBroj = random.nextInt(900) + 100;
        for (int i = 0; i < 4; i++) dostupniBrojevi[i] = random.nextInt(9) + 1;
        int[] srednji = {10, 15, 20};
        dostupniBrojevi[4] = srednji[random.nextInt(3)];
        int[] veliki = {25, 50, 75, 100};
        dostupniBrojevi[5] = veliki[random.nextInt(4)];
    }

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
            zadnjaX = x; zadnjaY = y; zadnjaZ = z;
            prvaPromjena = false;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (akcelerometar != null)
            sensorManager.registerListener(this, akcelerometar,
                    SensorManager.SENSOR_DELAY_NORMAL);
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