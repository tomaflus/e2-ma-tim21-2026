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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
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

    // Izazov mod
    private boolean jeIzazov = false;
    private String izazovId;

    // Brojevi
    private int ciljniBroj = 0;
    private int[] dostupniBrojevi = new int[6];
    private boolean ciljniOtkriven = false;
    private boolean brojeviOtkriveni = false;
    private boolean igraZavrsena = false;
    private int mojRezultat = 0;
    private int trenutnaRunda = 1;

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

    // Statusi (za partiju)
    private static final String OBA_IGRAJU = "OBA_IGRAJU";
    private static final String RACUNANJE_BODOVA = "RACUNANJE_BODOVA";
    private static final String RUNDA2 = "RUNDA2";
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
            izazovId = getArguments().getString("izazovId");
            jeIzazov = getArguments().getBoolean("jeIzazov", false);
        }

        inicijalizujIgru();

        // Slušaj status samo u partiji
        if (!jeIzazov) {
            slušajStatus();
        }

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
        if (jeIzazov) {
            // U izazovu — generiši brojeve lokalno, bez Firebase
            generisiBrojeve();
            pokrniTajmere();
        } else {
            // Normalna partija
            if (jeIgrac1) {
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
            pokrniTajmere();
        }
    }

    private void slušajStatus() {
        if (jeIzazov || partijaId == null) return;

        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("statusMojBroj");
                    if (status == null) return;

                    // Igrac 2 ucitava brojeve za Rundu 1
                    if (!jeIgrac1 && trenutnaRunda == 1 && !ciljniOtkriven) {
                        Long ciljni = snapshot.getLong("ciljniBrojRunda1");
                        List<Long> dostupni = (List<Long>) snapshot.get("dostupniBrojeviRunda1");
                        if (ciljni != null && dostupni != null && ciljniBroj == 0) {
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
                            izracunajBodove(snapshot);
                            break;

                        case RUNDA2:
                            trenutnaRunda = 2;
                            mojaTurnaZavrsena = false;
                            ciljniOtkriven = false;
                            brojeviOtkriveni = false;
                            tokeniIzraza.clear();
                            koristenaDugmad.clear();
                            mojRezultat = 0;

                            Long ciljni2 = snapshot.getLong("ciljniBrojRunda2");
                            List<Long> dostupni2 = (List<Long>) snapshot.get("dostupniBrojeviRunda2");

                            if (ciljni2 != null) ciljniBroj = ciljni2.intValue();
                            if (dostupni2 != null) {
                                for (int i = 0; i < dostupni2.size() && i < 6; i++) {
                                    dostupniBrojevi[i] = dostupni2.get(i).intValue();
                                }
                            }

                            resetujUI();
                            Toast.makeText(getContext(),
                                    "Runda 2 pocinje! Igrac 2 je vlasnik runde.",
                                    Toast.LENGTH_LONG).show();
                            pokrniTajmere();
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
                                    "Igra zavrsena! Ukupni bodovi: " + mojiB,
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

    private void resetujUI() {
        binding.etIzraz.setText("");
        binding.tvCiljniBroj.setText("---");
        binding.btnBroj1.setText("?"); binding.btnBroj1.setEnabled(true); binding.btnBroj1.setAlpha(1.0f);
        binding.btnBroj2.setText("?"); binding.btnBroj2.setEnabled(true); binding.btnBroj2.setAlpha(1.0f);
        binding.btnBroj3.setText("?"); binding.btnBroj3.setEnabled(true); binding.btnBroj3.setAlpha(1.0f);
        binding.btnBroj4.setText("?"); binding.btnBroj4.setEnabled(true); binding.btnBroj4.setAlpha(1.0f);
        binding.btnBroj5.setText("?"); binding.btnBroj5.setEnabled(true); binding.btnBroj5.setAlpha(1.0f);
        binding.btnBroj6.setText("?"); binding.btnBroj6.setEnabled(true); binding.btnBroj6.setAlpha(1.0f);
        binding.btnStopCiljni.setEnabled(true);
        binding.btnStopBrojevi.setEnabled(true);
        binding.btnPotvrdi.setEnabled(true);
        binding.tvTajmer.setText("60s");
        binding.tvBodovi.setText("Bodovi: 0");
    }

    private void izracunajBodove(DocumentSnapshot snapshot) {
        if (!jeIgrac1) return;

        Long r1, r2, ciljni;

        if (trenutnaRunda == 1) {
            r1 = snapshot.getLong("rezultat1Runda1");
            r2 = snapshot.getLong("rezultat2Runda1");
            ciljni = snapshot.getLong("ciljniBrojRunda1");
        } else {
            r1 = snapshot.getLong("rezultat1Runda2");
            r2 = snapshot.getLong("rezultat2Runda2");
            ciljni = snapshot.getLong("ciljniBrojRunda2");
        }

        if (r1 == null || r2 == null || ciljni == null) return;

        int rez1 = r1.intValue();
        int rez2 = r2.intValue();
        int cilj = ciljni.intValue();
        int bod1 = 0, bod2 = 0;

        if (rez1 == cilj && rez2 != cilj) {
            bod1 = 10;
        } else if (rez2 == cilj && rez1 != cilj) {
            bod2 = 10;
        } else if (rez1 == cilj && rez2 == cilj) {
            bod1 = 10; bod2 = 10;
        } else if (rez1 == 0 && rez2 == 0) {
            bod1 = 0; bod2 = 0;
        } else if (rez1 == 0) {
            bod2 = 5;
        } else if (rez2 == 0) {
            bod1 = 5;
        } else if (rez1 == rez2) {
            if (trenutnaRunda == 1) bod1 = 5;
            else bod2 = 5;
        } else {
            int razl1 = Math.abs(cilj - rez1);
            int razl2 = Math.abs(cilj - rez2);
            if (razl1 < razl2) bod1 = 5;
            else if (razl2 < razl1) bod2 = 5;
        }

        Long trenutniBod1 = snapshot.getLong("bodovi1MojBroj");
        Long trenutniBod2 = snapshot.getLong("bodovi2MojBroj");
        int ukupno1 = (trenutniBod1 != null ? trenutniBod1.intValue() : 0) + bod1;
        int ukupno2 = (trenutniBod2 != null ? trenutniBod2.intValue() : 0) + bod2;

        if (trenutnaRunda == 1) {
            generisiBrojeve();
            List<Integer> lista = new ArrayList<>();
            for (int b : dostupniBrojevi) lista.add(b);

            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(
                            "bodovi1MojBroj", ukupno1,
                            "bodovi2MojBroj", ukupno2,
                            "ciljniBrojRunda2", ciljniBroj,
                            "dostupniBrojeviRunda2", lista,
                            "rezultat1Runda2", -1,
                            "rezultat2Runda2", -1,
                            "statusMojBroj", RUNDA2
                    );
        } else {
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(
                            "bodovi1MojBroj", ukupno1,
                            "bodovi2MojBroj", ukupno2,
                            "statusMojBroj", ZAVRSENA
                    );
        }
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

        Toast.makeText(getContext(),
                "Rezultat: " + mojRezultat + " (ciljni: " + ciljniBroj + ")",
                Toast.LENGTH_SHORT).show();

        if (jeIzazov) {
            // U izazovu — direktno završi sa bodovima
            int bodovi = (mojRezultat == ciljniBroj) ? 10 : 0;
            igraZavrsena = true;
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> {
                        Bundle result = new Bundle();
                        result.putInt("bodovi", bodovi);
                        if (getParentFragmentManager() != null && isAdded()) {
                            getParentFragmentManager()
                                    .setFragmentResult("mojBrojZavrsen", result);
                        }
                    }, 1000);
        } else {
            // Normalna partija
            String poljeR;
            if (trenutnaRunda == 1) {
                poljeR = jeIgrac1 ? "rezultat1Runda1" : "rezultat2Runda1";
            } else {
                poljeR = jeIgrac1 ? "rezultat1Runda2" : "rezultat2Runda2";
            }

            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(poljeR, mojRezultat)
                    .addOnSuccessListener(unused -> provjeriDaLiObaZavrsili());
        }
    }

    private void provjeriDaLiObaZavrsili() {
        if (partijaId == null) return;

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Long r1, r2;
                    if (trenutnaRunda == 1) {
                        r1 = snapshot.getLong("rezultat1Runda1");
                        r2 = snapshot.getLong("rezultat2Runda1");
                    } else {
                        r1 = snapshot.getLong("rezultat1Runda2");
                        r2 = snapshot.getLong("rezultat2Runda2");
                    }

                    if (r1 != null && r2 != null && r1 != -1 && r2 != -1) {
                        FirebaseFirestore.getInstance()
                                .collection("partije").document(partijaId)
                                .update("statusMojBroj", RACUNANJE_BODOVA);
                    }
                });
    }

    private void pokrniTajmere() {
        zaustaviTajmere();

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
                if (!mojaTurnaZavrsena) {
                    mojaTurnaZavrsena = true;
                    binding.btnPotvrdi.setEnabled(false);
                    mojRezultat = 0;

                    if (jeIzazov) {
                        // U izazovu — završi sa 0 bodova
                        igraZavrsena = true;
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .postDelayed(() -> {
                                    Bundle result = new Bundle();
                                    result.putInt("bodovi", 0);
                                    if (getParentFragmentManager() != null && isAdded()) {
                                        getParentFragmentManager()
                                                .setFragmentResult("mojBrojZavrsen", result);
                                    }
                                }, 1000);
                    } else {
                        // Normalna partija
                        String poljeR;
                        if (trenutnaRunda == 1) {
                            poljeR = jeIgrac1 ? "rezultat1Runda1" : "rezultat2Runda1";
                        } else {
                            poljeR = jeIgrac1 ? "rezultat1Runda2" : "rezultat2Runda2";
                        }

                        FirebaseFirestore.getInstance()
                                .collection("partije").document(partijaId)
                                .update(poljeR, 0)
                                .addOnSuccessListener(unused -> provjeriDaLiObaZavrsili());
                    }
                }
                binding.tvTajmer.setText("0s");
            }
        }.start();
    }

    private void zaustaviTajmere() {
        if (tajmerGlavni != null) { tajmerGlavni.cancel(); tajmerGlavni = null; }
        if (tajmerCiljni != null) { tajmerCiljni.cancel(); tajmerCiljni = null; }
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
            Toast.makeText(getContext(), "Prvo otkrijte brojeve!", Toast.LENGTH_SHORT).show();
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
            while (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) {
                char op = expr.charAt(pos++);
                double term = parseTerm();
                result = (op == '+') ? result + term : result - term;
            }
            return result;
        }

        private double parseTerm() throws Exception {
            double result = parseFactor();
            while (pos < expr.length() && (expr.charAt(pos) == '*' || expr.charAt(pos) == '/')) {
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
                if (pos >= expr.length() || expr.charAt(pos) != ')') throw new Exception("Nedostaje )");
                pos++;
                return result;
            }
            boolean negative = false;
            if (pos < expr.length() && expr.charAt(pos) == '-') { negative = true; pos++; }
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
        if (prvaPromjena) { zadnjaX = x; zadnjaY = y; zadnjaZ = z; prvaPromjena = false; return; }
        float razlika = Math.abs(x - zadnjaX) + Math.abs(y - zadnjaY) + Math.abs(z - zadnjaZ);
        zadnjaX = x; zadnjaY = y; zadnjaZ = z;
        if (razlika > SHAKE_LIMIT) requireActivity().runOnUiThread(this::otkrijBrojeve);
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