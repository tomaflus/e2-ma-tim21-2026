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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MojBrojFragment extends Fragment implements SensorEventListener {

    private FragmentMojBrojBinding binding;
    private SensorManager sensorManager;
    private Sensor akcelerometar;
    private ListenerRegistration listenerRegistration;

    // Parametri partije
    private String partijaId;
    private boolean jeIgrac1;

    // Brojevi
    private int ciljniBroj = 0;
    private int[] dostupniBrojevi = new int[6];
    private boolean ciljniOtkriven = false;
    private boolean brojeviOtkriveni = false;
    private boolean igraZavrsena = false;
    private int bodovi = 0;
    private int mojRezultat = 0;

    // Tajmeri
    private CountDownTimer tajmerCiljni;
    private CountDownTimer tajmerGlavni;

    // Stack za token brisanje
    private Stack<String> tokeniIzraza = new Stack<>();
    private Stack<Button> koristenaDugmad = new Stack<>();

    // Shake
    private float zadnjaX, zadnjaY, zadnjaZ;
    private boolean prvaPromjena = true;
    private static final float SHAKE_LIMIT = 80;
    private static final int TRAJANJE_IGRE = 60000;
    private static final int TRAJANJE_STOP = 5000;

    // Statusi
    private static final String IGRAC1_IGRA = "IGRAC1_IGRA";
    private static final String IGRAC2_IGRA = "IGRAC2_IGRA";
    private static final String OBA_IGRAJU = "OBA_IGRAJU";
    private static final String RACUNANJE_BODOVA = "RACUNANJE_BODOVA";
    private static final String ZAVRSENA = "ZAVRSENA";

    private String trenutniStatus = "";
    private boolean mojaTurnaZavrsena = false;

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

        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
        }

        inicijalizujIgru();
        slušajStatus();

        binding.btnStopCiljni.setOnClickListener(v -> otkrijCiljniBroj());
        binding.btnStopBrojevi.setOnClickListener(v -> otkrijBrojeve());

        binding.btnSaberi.setOnClickListener(v -> dodajToken(" + ", null));
        binding.btnOduzmi.setOnClickListener(v -> dodajToken(" - ", null));
        binding.btnPomnozi.setOnClickListener(v -> dodajToken(" * ", null));
        binding.btnPodijeli.setOnClickListener(v -> dodajToken(" / ", null));
        binding.btnZagraOtvori.setOnClickListener(v -> dodajToken("(", null));
        binding.btnZagraZatvori.setOnClickListener(v -> dodajToken(")", null));

        binding.btnBroj1.setOnClickListener(v -> klikniNaBroj(binding.btnBroj1));
        binding.btnBroj2.setOnClickListener(v -> klikniNaBroj(binding.btnBroj2));
        binding.btnBroj3.setOnClickListener(v -> klikniNaBroj(binding.btnBroj3));
        binding.btnBroj4.setOnClickListener(v -> klikniNaBroj(binding.btnBroj4));
        binding.btnBroj5.setOnClickListener(v -> klikniNaBroj(binding.btnBroj5));
        binding.btnBroj6.setOnClickListener(v -> klikniNaBroj(binding.btnBroj6));

        binding.btnObrisi.setOnClickListener(v -> {
            if (tokeniIzraza.isEmpty()) return;
            String zadnjiToken = tokeniIzraza.pop();
            Button dugme = koristenaDugmad.pop();
            if (dugme != null) {
                dugme.setEnabled(true);
                dugme.setAlpha(1.0f);
            }
            String trenutni = binding.etIzraz.getText().toString();
            if (trenutni.endsWith(zadnjiToken)) {
                binding.etIzraz.setText(
                        trenutni.substring(0, trenutni.length() - zadnjiToken.length()));
                binding.etIzraz.setSelection(binding.etIzraz.getText().length());
            }
        });

        binding.btnPotvrdi.setOnClickListener(v -> {
            if (igraZavrsena || mojaTurnaZavrsena) return;
            String izraz = binding.etIzraz.getText().toString().trim();
            if (izraz.isEmpty()) {
                Toast.makeText(getContext(), "Unesite izraz!", Toast.LENGTH_SHORT).show();
                return;
            }
            potvrdiRezultat(izraz);
        });
    }

    private void inicijalizujIgru() {
        if (jeIgrac1) {
            // Igrac 1 generise brojeve i cuva u Firebase
            generisiBrojeve();

            List<Integer> lista = new ArrayList<>();
            for (int b : dostupniBrojevi) lista.add(b);

            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(
                            "statusMojBroj", OBA_IGRAJU,
                            "ciljniBrojRunda1", ciljniBroj,
                            "dostupniBrojeviRunda1", lista,
                            "bodovi1MojBroj", 0,
                            "bodovi2MojBroj", 0,
                            "rezultat1Runda1", -1,
                            "rezultat2Runda1", -1
                    );
        }
        // Igrac 2 ucitava brojeve iz Firebase u slušajStatus()

        pokrniTajmere();
    }

    private void slušajStatus() {
        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("statusMojBroj");
                    if (status == null) return;

                    // Igrac 2 ucitava brojeve kad su dostupni
                    if (!jeIgrac1 && !brojeviOtkriveni && !ciljniOtkriven) {
                        Long ciljni = snapshot.getLong("ciljniBrojRunda1");
                        List<Long> dostupni = (List<Long>) snapshot.get("dostupniBrojeviRunda1");
                        if (ciljni != null && dostupni != null) {
                            ciljniBroj = ciljni.intValue();
                            for (int i = 0; i < dostupni.size() && i < 6; i++) {
                                dostupniBrojevi[i] = dostupni.get(i).intValue();
                            }
                        }
                    }

                    if (status.equals(trenutniStatus)) return;
                    trenutniStatus = status;

                    switch (status) {
                        case RACUNANJE_BODOVA:
                            // Oba igraca su zavrsili — racunaj bodove
                            izracunajBodove(snapshot);
                            break;

                        case ZAVRSENA:
                            Long b1 = snapshot.getLong("bodovi1MojBroj");
                            Long b2 = snapshot.getLong("bodovi2MojBroj");
                            int mojiB = jeIgrac1 ?
                                    (b1 != null ? b1.intValue() : 0) :
                                    (b2 != null ? b2.intValue() : 0);

                            zaustaviTajmere();
                            igraZavrsena = true;
                            binding.btnPotvrdi.setEnabled(false);

                            Toast.makeText(getContext(),
                                    "Igra zavrsena! Bodovi: " + mojiB,
                                    Toast.LENGTH_LONG).show();

                            Bundle result = new Bundle();
                            result.putInt("bodovi", mojiB);
                            if (getParentFragmentManager() != null) {
                                getParentFragmentManager()
                                        .setFragmentResult("mojBrojZavrsen", result);
                            }
                            break;
                    }
                });
    }

    private void izracunajBodove(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (!jeIgrac1) return; // Samo Igrac 1 racuna bodove

        Long r1 = snapshot.getLong("rezultat1Runda1");
        Long r2 = snapshot.getLong("rezultat2Runda1");
        Long ciljni = snapshot.getLong("ciljniBrojRunda1");

        if (r1 == null || r2 == null || ciljni == null) return;

        int rez1 = r1.intValue();
        int rez2 = r2.intValue();
        int cilj = ciljni.intValue();
        int bod1 = 0, bod2 = 0;

        if (rez1 == cilj && rez2 != cilj) {
            // Igrac 1 pogodio, Igrac 2 nije
            bod1 = 10;
        } else if (rez2 == cilj && rez1 != cilj) {
            // Igrac 2 pogodio, Igrac 1 nije
            bod2 = 10;
        } else if (rez1 == cilj && rez2 == cilj) {
            // Oba pogodila
            bod1 = 10;
            bod2 = 10;
        } else if (rez1 == 0 && rez2 == 0) {
            // Nijedan nije unio nista
            bod1 = 0;
            bod2 = 0;
        } else if (rez1 == 0) {
            // Igrac 1 nije unio nista
            bod2 = 5;
        } else if (rez2 == 0) {
            // Igrac 2 nije unio nista
            bod1 = 5;
        } else if (rez1 == rez2) {
            // Isti netacni rezultat — bodove dobija Igrac 1 (cija je runda)
            bod1 = 5;
        } else {
            // Ko je blizi
            int razl1 = Math.abs(cilj - rez1);
            int razl2 = Math.abs(cilj - rez2);
            if (razl1 < razl2) bod1 = 5;
            else if (razl2 < razl1) bod2 = 5;
        }

        int finalBod1 = bod1;
        int finalBod2 = bod2;

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update(
                        "bodovi1MojBroj", finalBod1,
                        "bodovi2MojBroj", finalBod2,
                        "statusMojBroj", ZAVRSENA
                );
    }

    private void potvrdiRezultat(String izraz) {
        try {
            double rezultat = new ExprParser(izraz.replaceAll("\\s+", "")).parse();
            mojRezultat = (int) rezultat;
        } catch (Exception ex) {
            mojRezultat = 0;
        }

        mojaTurnaZavrsena = true;
        zaustaviTajmere();
        binding.btnPotvrdi.setEnabled(false);

        String poljeR = jeIgrac1 ? "rezultat1Runda1" : "rezultat2Runda1";

        Toast.makeText(getContext(),
                "Rezultat: " + mojRezultat + " (ciljni: " + ciljniBroj + ")",
                Toast.LENGTH_SHORT).show();

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update(poljeR, mojRezultat)
                .addOnSuccessListener(unused -> provjeriDaLiObaZavrsili());
    }

    private void provjeriDaLiObaZavrsili() {
        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Long r1 = snapshot.getLong("rezultat1Runda1");
                    Long r2 = snapshot.getLong("rezultat2Runda1");

                    // Ako oba igraca imaju rezultat (nije -1) — racunaj bodove
                    if (r1 != null && r2 != null && r1 != -1 && r2 != -1) {
                        FirebaseFirestore.getInstance()
                                .collection("partije").document(partijaId)
                                .update("statusMojBroj", RACUNANJE_BODOVA);
                    }
                });
    }

    private void pokrniTajmere() {
        // Tajmer 5s za auto-otkrivanje ciljnog broja
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

        // Glavni tajmer 60s
        tajmerGlavni = new CountDownTimer(TRAJANJE_IGRE, 1000) {
            @Override
            public void onTick(long ms) {
                binding.tvTajmer.setText(ms / 1000 + "s");
            }
            @Override
            public void onFinish() {
                if (!mojaTurnaZavrsena) {
                    // Igrac nije unio nista — postavi 0
                    mojaTurnaZavrsena = true;
                    binding.btnPotvrdi.setEnabled(false);
                    mojRezultat = 0;

                    String poljeR = jeIgrac1 ? "rezultat1Runda1" : "rezultat2Runda1";
                    FirebaseFirestore.getInstance()
                            .collection("partije").document(partijaId)
                            .update(poljeR, 0)
                            .addOnSuccessListener(unused -> provjeriDaLiObaZavrsili());
                }
                binding.tvTajmer.setText("0s");
            }
        }.start();
    }

    private void zaustaviTajmere() {
        if (tajmerGlavni != null) tajmerGlavni.cancel();
        if (tajmerCiljni != null) tajmerCiljni.cancel();
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

    private void klikniNaBroj(Button dugme) {
        if (!brojeviOtkriveni) {
            Toast.makeText(getContext(),
                    "Prvo otkrijte brojeve!", Toast.LENGTH_SHORT).show();
            return;
        }
        String broj = dugme.getText().toString();
        dodajToken(broj, dugme);
        dugme.setEnabled(false);
        dugme.setAlpha(0.4f);
    }

    private void dodajToken(String token, Button dugme) {
        String trenutni = binding.etIzraz.getText().toString();
        binding.etIzraz.setText(trenutni + token);
        binding.etIzraz.setSelection(binding.etIzraz.getText().length());
        tokeniIzraza.push(token);
        koristenaDugmad.push(dugme);
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

    // Rekurzivni parser
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
                    throw new Exception("Nedostaje )");
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
            if (pos == start) throw new Exception("Ocekivan broj");
            double value = Double.parseDouble(expr.substring(start, pos));
            return negative ? -value : value;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        if (prvaPromjena) {
            zadnjaX = x; zadnjaY = y; zadnjaZ = z;
            prvaPromjena = false;
            return;
        }
        float razlika = Math.abs(x - zadnjaX) + Math.abs(y - zadnjaY) + Math.abs(z - zadnjaZ);
        zadnjaX = x; zadnjaY = y; zadnjaZ = z;
        if (razlika > SHAKE_LIMIT) {
            requireActivity().runOnUiThread(this::otkrijBrojeve);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (akcelerometar != null)
            sensorManager.registerListener(this, akcelerometar, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        zaustaviTajmere();
        if (listenerRegistration != null) listenerRegistration.remove();
        binding = null;
    }
}