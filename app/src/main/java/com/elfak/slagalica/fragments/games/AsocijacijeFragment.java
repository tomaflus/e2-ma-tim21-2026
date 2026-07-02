package com.elfak.slagalica.fragments.games;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentAsocijacijeBinding;
import com.elfak.slagalica.model.Asocijacija;
import com.elfak.slagalica.repository.AsocijacijeRepository;
import com.elfak.slagalica.viewModels.games.AsocijacijeViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsocijacijeFragment extends Fragment {

    private static final long TRAJANJE_RUNDE = 120_000;

    private static final int BOJA_SKRIVENO = Color.parseColor("#A8D0EC");
    private static final int BOJA_IGRAC1   = Color.parseColor("#C0392B");
    private static final int BOJA_IGRAC2   = Color.parseColor("#1565C0");
    private static final int BOJA_POGRESNO = Color.parseColor("#757575");
    private static final int BOJA_NEUTRAL  = Color.parseColor("#9E9E9E");

    private static final String RUNDA1_IGRAC1_IGRA     = "RUNDA1_IGRAC1_IGRA";
    private static final String RUNDA1_IGRAC2_IGRA     = "RUNDA1_IGRAC2_IGRA";
    private static final String RUNDA1_PAUZA           = "RUNDA1_PAUZA";
    private static final String RUNDA2_INICIJALIZACIJA = "RUNDA2_INICIJALIZACIJA";
    private static final String RUNDA2_IGRAC2_IGRA     = "RUNDA2_IGRAC2_IGRA";
    private static final String RUNDA2_IGRAC1_IGRA     = "RUNDA2_IGRAC1_IGRA";
    private static final String RUNDA2_PAUZA           = "RUNDA2_PAUZA";
    private static final String ZAVRSENA               = "ZAVRSENA";

    private static final String[] SLOVA = {"A", "B", "C", "D"};

    private FragmentAsocijacijeBinding binding;

    private String partijaId;
    private boolean jeIgrac1;
    private boolean jeIzazov;

    private com.google.android.material.button.MaterialButton[][] itemDugmad;
    private com.google.android.material.button.MaterialButton[] rezDugmad;

    // Solo
    private AsocijacijeViewModel viewModel;
    private CountDownTimer tajmer;

    // Multi
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private AsocijacijeRepository asocRepo;
    private Asocijacija lokalnoPitanjeR1;
    private Asocijacija lokalnoPitanjeR2;
    private boolean jeRunda2 = false;
    private boolean mojaRunda = false;

    private List<Long> vlasnikPolja;
    private List<Long> vlasnikKolone;
    private long vlasnikKonacno = 0L;

    private int bodovi1Multi = 0;
    private int bodovi2Multi = 0;
    private int startingScore1 = 0;
    private int startingScore2 = 0;
    private long currentTimerEndMs = 0;

    private boolean mojePoljeOtvoreno = false;
    private String lastSeenStatus = null;
    private boolean rezultatPoslan = false;

    // Pokušaj odgovora vidljiv protivniku
    private int pokusajKolonaCached = Integer.MIN_VALUE;
    private String pokusajTekstCached = null;
    private boolean pokusajVSecu = false;

    // Odbrojavanje između rundi
    private boolean odbrojavanjeAktivno = false;
    private long lastToastTsAsoc = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAsocijacijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            partijaId      = getArguments().getString("partijaId");
            jeIgrac1       = getArguments().getBoolean("jeIgrac1", true);
            jeIzazov       = getArguments().getBoolean("jeIzazov", false);
            startingScore1 = getArguments().getInt("startingScore1", 0);
            startingScore2 = getArguments().getInt("startingScore2", 0);
        }

        inicijalizujDugmadReference();

        if (jeIzazov) {
            inicijalizujSolo();
        } else {
            db       = FirebaseFirestore.getInstance();
            asocRepo = new AsocijacijeRepository();
            inicijalizujMultiplayer();
        }
    }

    // ─── SOLO PATH ────────────────────────────────────────

    private void inicijalizujSolo() {
        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);
        postaviListenereSolo();
        obnavljanjeUISolo();
        obnavljanjeTajmeraSolo();
    }

    private void postaviListenereSolo() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                final int kolona = k, red = r;
                itemDugmad[k][r].setOnClickListener(v -> {
                    if (!viewModel.zavrsen && !viewModel.otvorenaPolja[kolona][red]) {
                        viewModel.otvorenaPolja[kolona][red] = true;
                        otkriPoljeBoja(kolona, red, AsocijacijeViewModel.POLJA[kolona][red], BOJA_IGRAC1);
                    }
                });
            }
            final int kolona = k;
            rezDugmad[k].setOnClickListener(v -> {
                if (!viewModel.zavrsen && !viewModel.pogodenaKolona[kolona])
                    prikaziDialogSolo(kolona);
            });
        }
        binding.btnKonacno.setOnClickListener(v -> {
            if (!viewModel.zavrsen && !viewModel.konacnoPogodeno) prikaziDialogSolo(-1);
        });
    }

    private void obnavljanjeUISolo() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++)
                if (viewModel.otvorenaPolja[k][r])
                    otkriPoljeBoja(k, r, AsocijacijeViewModel.POLJA[k][r], BOJA_IGRAC1);
            if (viewModel.pogodenaKolona[k])
                oznaciKolonuBoja(k, AsocijacijeViewModel.RJESENJA_KOLONA[k], BOJA_IGRAC1);
        }
        if (viewModel.konacnoPogodeno)
            oznaciKonacnoBoja(AsocijacijeViewModel.KONACNO_RJESENJE, BOJA_IGRAC1);
        if (viewModel.zavrsen) onemogucuiSvePolja();
    }

    private void obnavljanjeTajmeraSolo() {
        if (viewModel.zavrsen) { binding.tvTajmer.setText("⏱ 0:00"); return; }
        long sada = System.currentTimeMillis();
        if (viewModel.timerEndMs == 0) viewModel.timerEndMs = sada + TRAJANJE_RUNDE;
        long preostalo = viewModel.timerEndMs - sada;
        if (preostalo > 0) startTajmerSolo(preostalo);
        else {
            viewModel.zavrsen = true;
            binding.tvTajmer.setText("⏱ 0:00");
            onemogucuiSvePolja();
            posaljiRezultatSolo();
        }
    }

    private void startTajmerSolo(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                long sec = ms / 1000;
                binding.tvTajmer.setText(String.format("⏱ %d:%02d", sec / 60, sec % 60));
            }
            @Override public void onFinish() {
                viewModel.zavrsen = true;
                binding.tvTajmer.setText("⏱ 0:00");
                onemogucuiSvePolja();
                posaljiRezultatSolo();
            }
        }.start();
    }

    private void prikaziDialogSolo(int kolona) {
        String naslov = kolona == -1 ? "Konačno rešenje"
                : "Rešenje kolone " + AsocijacijeViewModel.SLOVA_KOLONA[kolona];
        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("Unesite odgovor...");
        new AlertDialog.Builder(requireContext()).setTitle(naslov).setView(et)
                .setPositiveButton("Pogodi", (d, w) ->
                        provjeriOdgovorSolo(kolona, et.getText().toString().trim()))
                .setNegativeButton("Odustani", null).show();
    }

    private void provjeriOdgovorSolo(int kolona, String unos) {
        String tacno = kolona == -1
                ? AsocijacijeViewModel.KONACNO_RJESENJE
                : AsocijacijeViewModel.RJESENJA_KOLONA[kolona];
        if (!unos.equalsIgnoreCase(tacno)) {
            Toast.makeText(getContext(), "Netačno!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (kolona == -1) {
            viewModel.konacnoPogodeno = true;
            viewModel.zavrsen = true;
            if (tajmer != null) tajmer.cancel();
            dodajBodoveSolo(izracunajBodoveKonacnoSolo());
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++)
                    if (!viewModel.otvorenaPolja[k][r]) {
                        viewModel.otvorenaPolja[k][r] = true;
                        otkriPoljeBoja(k, r, AsocijacijeViewModel.POLJA[k][r], BOJA_IGRAC1);
                    }
                if (!viewModel.pogodenaKolona[k]) {
                    viewModel.pogodenaKolona[k] = true;
                    oznaciKolonuBoja(k, AsocijacijeViewModel.RJESENJA_KOLONA[k], BOJA_IGRAC1);
                }
            }
            oznaciKonacnoBoja(AsocijacijeViewModel.KONACNO_RJESENJE, BOJA_IGRAC1);
            posaljiRezultatSolo();
        } else {
            viewModel.pogodenaKolona[kolona] = true;
            dodajBodoveSolo(izracunajBodoveKoloneSolo(kolona));
            for (int r = 0; r < 4; r++)
                if (!viewModel.otvorenaPolja[kolona][r]) {
                    viewModel.otvorenaPolja[kolona][r] = true;
                    otkriPoljeBoja(kolona, r, AsocijacijeViewModel.POLJA[kolona][r], BOJA_IGRAC1);
                }
            oznaciKolonuBoja(kolona, AsocijacijeViewModel.RJESENJA_KOLONA[kolona], BOJA_IGRAC1);
        }
    }

    private void dodajBodoveSolo(int bodi) {
        Integer t = viewModel.score.getValue();
        viewModel.score.setValue((t == null ? 0 : t) + bodi);
    }

    private int izracunajBodoveKoloneSolo(int k) {
        int neotv = 0;
        for (int r = 0; r < 4; r++) if (!viewModel.otvorenaPolja[k][r]) neotv++;
        return 2 + neotv;
    }

    private int izracunajBodoveKonacnoSolo() {
        int bodi = 7;
        for (int k = 0; k < 4; k++) {
            if (!viewModel.pogodenaKolona[k]) {
                boolean imaOtv = false;
                for (int r = 0; r < 4; r++) if (viewModel.otvorenaPolja[k][r]) { imaOtv = true; break; }
                bodi += imaOtv ? izracunajBodoveKoloneSolo(k) : 6;
            }
        }
        return bodi;
    }

    private void onemogucuiSvePolja() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) itemDugmad[k][r].setEnabled(false);
            if (!viewModel.pogodenaKolona[k]) rezDugmad[k].setEnabled(false);
        }
        if (!viewModel.konacnoPogodeno) binding.btnKonacno.setEnabled(false);
    }

    private void posaljiRezultatSolo() {
        Integer sc = viewModel.score.getValue();
        Bundle result = new Bundle();
        result.putInt("bodovi", sc != null ? sc : 0);
        if (isAdded()) getParentFragmentManager().setFragmentResult("asocijacijeZavrsen", result);
    }

    // ─── MULTIPLAYER PATH ─────────────────────────────────

    private void inicijalizujMultiplayer() {
        vlasnikPolja  = new ArrayList<>(Collections.nCopies(16, 0L));
        vlasnikKolone = new ArrayList<>(Collections.nCopies(4, 0L));

        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.btnPredajPotez.setVisibility(View.VISIBLE);
        binding.btnPredajPotez.setOnClickListener(v -> predajPotez());

        postaviListenereMultiplayer();
        setOverlay(true);
        prikaziStatusMulti("Učitavanje...");

        slušajPartijuMulti();
        if (jeIgrac1) inicijalizujRunda1KaoIgrac1();
    }

    private void inicijalizujRunda1KaoIgrac1() {
        asocRepo.dohvatiNasumicnoPitanje(pitanje -> {
            if (!isAdded()) return;
            lokalnoPitanjeR1 = pitanje;
            long timerEnd = System.currentTimeMillis() + TRAJANJE_RUNDE;
            db.collection("partije").document(partijaId).update(
                    "pitanjeAsocijacijeIdR1",  pitanje.getId(),
                    "vlasnikPoljaR1",          new ArrayList<>(Collections.nCopies(16, 0L)),
                    "vlasnikKoloneR1",         new ArrayList<>(Collections.nCopies(4, 0L)),
                    "vlasnikKonacnoR1",        0L,
                    "bodovi1Asocijacije",      0,
                    "bodovi2Asocijacije",      0,
                    "bodovi1",                 startingScore1,
                    "bodovi2",                 startingScore2,
                    "timerEndMsAsocijacijeR1", timerEnd,
                    "statusAsocijacije",       RUNDA1_IGRAC1_IGRA
            );
        }, err -> Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show());
    }

    private void inicijalizujRunda2KaoIgrac2(String r1Id) {
        asocRepo.dohvatiNasumicnoPitanjeIzuzimajuci(r1Id, pitanje -> {
            if (!isAdded()) return;
            lokalnoPitanjeR2 = pitanje;
            long timerEnd = System.currentTimeMillis() + TRAJANJE_RUNDE;
            db.collection("partije").document(partijaId).update(
                    "pitanjeAsocijacijeIdR2",  pitanje.getId(),
                    "vlasnikPoljaR2",          new ArrayList<>(Collections.nCopies(16, 0L)),
                    "vlasnikKoloneR2",         new ArrayList<>(Collections.nCopies(4, 0L)),
                    "vlasnikKonacnoR2",        0L,
                    "timerEndMsAsocijacijeR2", timerEnd,
                    "statusAsocijacije",       RUNDA2_IGRAC2_IGRA
            );
        }, err -> Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show());
    }

    private void postaviListenereMultiplayer() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                final int kolona = k, red = r;
                final int idx = k * 4 + r;
                itemDugmad[k][r].setOnClickListener(v -> {
                    if (!mojaRunda || mojePoljeOtvoreno || pokusajVSecu) return;
                    if (vlasnikPolja.get(idx) != 0) return;
                    Asocijacija pit = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
                    if (pit == null) return;

                    long vlasnik = jeIgrac1 ? 1L : 2L;
                    vlasnikPolja.set(idx, vlasnik);
                    mojePoljeOtvoreno = true;
                    otkriPoljeBoja(kolona, red, pit.getPolja()[kolona][red],
                            vlasnik == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2);
                    osveziDugmadMulti();

                    String f = jeRunda2 ? "vlasnikPoljaR2" : "vlasnikPoljaR1";
                    db.collection("partije").document(partijaId)
                            .update(f, new ArrayList<>(vlasnikPolja));
                });
            }
            final int kolona = k;
            rezDugmad[k].setOnClickListener(v -> {
                if (!mojaRunda || !mojePoljeOtvoreno || pokusajVSecu) return;
                if (vlasnikKolone.get(kolona) != 0) return;
                Asocijacija pit = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
                if (pit != null) prikaziDialogMulti(kolona, pit);
            });
        }
        binding.btnKonacno.setOnClickListener(v -> {
            if (!mojaRunda || !mojePoljeOtvoreno || pokusajVSecu || vlasnikKonacno != 0) return;
            Asocijacija pit = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
            if (pit != null) prikaziDialogMulti(-1, pit);
        });
    }

    private void prikaziDialogMulti(int kolona, Asocijacija pitanje) {
        String naslov = kolona == -1 ? "Konačno rešenje" : "Rešenje kolone " + SLOVA[kolona];
        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("Unesite odgovor...");
        new AlertDialog.Builder(requireContext()).setTitle(naslov).setView(et)
                .setPositiveButton("Pogodi", (d, w) ->
                        provjeriOdgovorMulti(kolona, et.getText().toString().trim(), pitanje))
                .setNegativeButton("Odustani", null).show();
    }

    private void provjeriOdgovorMulti(int kolona, String unos, Asocijacija pitanje) {
        String tacno = kolona == -1
                ? pitanje.getKonacnoRjesenje()
                : pitanje.getRjesenja()[kolona];

        if (!unos.equalsIgnoreCase(tacno)) {
            prikaziPogresnoPa1sPredaj(kolona, unos);
            return;
        }

        long vlasnik = jeIgrac1 ? 1L : 2L;
        int bojaInt  = vlasnik == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2;

        if (kolona == -1) {
            vlasnikKonacno = vlasnik;
            int bodi    = izracunajBodoveKonacnoMulti();
            String poljeB = jeIgrac1 ? "bodovi1Asocijacije" : "bodovi2Asocijacije";
            int noviB   = (jeIgrac1 ? bodovi1Multi : bodovi2Multi) + bodi;
            if (jeIgrac1) bodovi1Multi = noviB; else bodovi2Multi = noviB;
            String pauza = jeRunda2 ? RUNDA2_PAUZA : RUNDA1_PAUZA;
            String poljeKon = jeRunda2 ? "vlasnikKonacnoR2" : "vlasnikKonacnoR1";
            String poljeOtv = jeRunda2 ? "vlasnikPoljaR2"   : "vlasnikPoljaR1";
            String poljeKol = jeRunda2 ? "vlasnikKoloneR2"  : "vlasnikKoloneR1";

            otkriSvaPoljaIKolone(pitanje, vlasnik);
            oznaciKonacnoBoja(pitanje.getKonacnoRjesenje(), bojaInt);

            Map<String, Object> updKon = new HashMap<>();
            updKon.put(poljeB,   noviB);
            updKon.put(poljeKon, vlasnik);
            updKon.put(poljeOtv, new ArrayList<>(Collections.nCopies(16, vlasnik)));
            updKon.put(poljeKol, new ArrayList<>(Collections.nCopies(4, vlasnik)));
            updKon.put("statusAsocijacije", pauza);
            if (jeIgrac1) updKon.put("bodovi1", startingScore1 + bodovi1Multi);
            else          updKon.put("bodovi2", startingScore2 + bodovi2Multi);
            updKon.put("toastMsgAsoc", "Konačno! +" + bodi + " bodova");
            updKon.put("toastTsAsoc", System.currentTimeMillis());
            db.collection("partije").document(partijaId).update(updKon);
        } else {
            vlasnikKolone.set(kolona, vlasnik);
            int bodi  = izracunajBodoveKoloneMulti(kolona);
            String poljeB = jeIgrac1 ? "bodovi1Asocijacije" : "bodovi2Asocijacije";
            int noviB = (jeIgrac1 ? bodovi1Multi : bodovi2Multi) + bodi;
            if (jeIgrac1) bodovi1Multi = noviB; else bodovi2Multi = noviB;

            for (int r = 0; r < 4; r++) vlasnikPolja.set(kolona * 4 + r, vlasnik);
            for (int r = 0; r < 4; r++)
                otkriPoljeBoja(kolona, r, pitanje.getPolja()[kolona][r], bojaInt);
            oznaciKolonuBoja(kolona, pitanje.getRjesenja()[kolona], bojaInt);

            String poljeOtv = jeRunda2 ? "vlasnikPoljaR2"  : "vlasnikPoljaR1";
            String poljeKol = jeRunda2 ? "vlasnikKoloneR2" : "vlasnikKoloneR1";
            Map<String, Object> updKol = new HashMap<>();
            updKol.put(poljeB,   noviB);
            updKol.put(poljeOtv, new ArrayList<>(vlasnikPolja));
            updKol.put(poljeKol, new ArrayList<>(vlasnikKolone));
            if (jeIgrac1) updKol.put("bodovi1", startingScore1 + bodovi1Multi);
            else          updKol.put("bodovi2", startingScore2 + bodovi2Multi);
            updKol.put("toastMsgAsoc", "Tačan odgovor! +" + bodi + " bodova");
            updKol.put("toastTsAsoc", System.currentTimeMillis());
            db.collection("partije").document(partijaId).update(updKol);
            osveziDugmadMulti();
        }
    }

    private void prikaziPogresnoPa1sPredaj(int kolona, String unos) {
        pokusajVSecu = true;
        osveziDugmadMulti();

        // Odmah lokalno prikaži sivi odgovor
        applyPokusajVizuelno(kolona, unos);

        // Upiši u Firestore da protivnik vidi kroz overlay
        String kolonaField = jeRunda2 ? "pokusajKolonaR2" : "pokusajKolonaR1";
        String tekstField  = jeRunda2 ? "pokusajTekstR2"  : "pokusajTekstR1";
        db.collection("partije").document(partijaId)
                .update(kolonaField, (long) kolona, tekstField, unos,
                        "toastMsgAsoc", "Netačan odgovor!",
                        "toastTsAsoc", System.currentTimeMillis());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            pokusajVSecu = false;
            pokusajKolonaCached = Integer.MIN_VALUE;
            pokusajTekstCached  = null;
            resetPokusajVizuelno(kolona);

            Map<String, Object> upd = new HashMap<>();
            upd.put("statusAsocijacije", sledeciStatus());
            upd.put(kolonaField, FieldValue.delete());
            upd.put(tekstField,  FieldValue.delete());
            db.collection("partije").document(partijaId).update(upd);
        }, 1000);
    }

    private void applyPokusajVizuelno(int kolona, String tekst) {
        if (kolona == -1) {
            binding.btnKonacno.setText(tekst);
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_POGRESNO));
            binding.btnKonacno.setTextColor(Color.WHITE);
        } else if (kolona >= 0 && kolona < 4) {
            rezDugmad[kolona].setText(tekst);
            rezDugmad[kolona].setBackgroundTintList(ColorStateList.valueOf(BOJA_POGRESNO));
            rezDugmad[kolona].setTextColor(Color.WHITE);
        }
    }

    private void resetPokusajVizuelno(int kolona) {
        if (kolona == -1) {
            if (vlasnikKonacno == 0) {
                binding.btnKonacno.setText("???");
                binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                binding.btnKonacno.setTextColor(Color.parseColor("#12205A"));
            }
        } else if (kolona >= 0 && kolona < 4 && vlasnikKolone.get(kolona) == 0) {
            rezDugmad[kolona].setText(SLOVA[kolona]);
            rezDugmad[kolona].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
            rezDugmad[kolona].setTextColor(Color.parseColor("#12205A"));
        }
    }

    private void predajPotez() {
        db.collection("partije").document(partijaId)
                .update("statusAsocijacije", sledeciStatus());
    }

    private String sledeciStatus() {
        if (!jeRunda2) return jeIgrac1 ? RUNDA1_IGRAC2_IGRA : RUNDA1_IGRAC1_IGRA;
        else           return jeIgrac1 ? RUNDA2_IGRAC2_IGRA : RUNDA2_IGRAC1_IGRA;
    }

    @SuppressWarnings("unchecked")
    private void slušajPartijuMulti() {
        listenerReg = db.collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists() || !isAdded()) return;

                    String status = snapshot.getString("statusAsocijacije");
                    if (status == null) return;

                    boolean statusChanged = !status.equals(lastSeenStatus);
                    lastSeenStatus = status;

                    Long b1L = snapshot.getLong("bodovi1Asocijacije");
                    Long b2L = snapshot.getLong("bodovi2Asocijacije");
                    bodovi1Multi = b1L != null ? b1L.intValue() : 0;
                    bodovi2Multi = b2L != null ? b2L.intValue() : 0;

                    Long toastTs = snapshot.getLong("toastTsAsoc");
                    if (toastTs != null && toastTs != lastToastTsAsoc) {
                        lastToastTsAsoc = toastTs;
                        String msg = snapshot.getString("toastMsgAsoc");
                        if (msg != null && !msg.isEmpty() && isAdded())
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    switch (status) {
                        case RUNDA1_IGRAC1_IGRA:
                        case RUNDA1_IGRAC2_IGRA:
                        case RUNDA1_PAUZA:
                            prosesujRunda1(snapshot, status, statusChanged);
                            break;

                        case RUNDA2_INICIJALIZACIJA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            setOverlay(true);
                            prikaziStatusMulti("Priprema 2. runde...");
                            if (!jeIgrac1) {
                                resetBoardMulti();
                                inicijalizujRunda2KaoIgrac2(snapshot.getString("pitanjeAsocijacijeIdR1"));
                            }
                            break;

                        case RUNDA2_IGRAC2_IGRA:
                        case RUNDA2_IGRAC1_IGRA:
                        case RUNDA2_PAUZA:
                            prosesujRunda2(snapshot, status, statusChanged);
                            break;

                        case ZAVRSENA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            setOverlay(true);
                            if (!rezultatPoslan) {
                                rezultatPoslan = true;
                                int mojiBodovi = jeIgrac1 ? bodovi1Multi : bodovi2Multi;
                                Bundle result = new Bundle();
                                result.putInt("bodovi", mojiBodovi);
                                if (isAdded())
                                    getParentFragmentManager()
                                            .setFragmentResult("asocijacijeZavrsen", result);
                            }
                            break;
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void prosesujRunda1(DocumentSnapshot snapshot, String status, boolean statusChanged) {
        jeRunda2 = false;
        List<Long> vp = (List<Long>) snapshot.get("vlasnikPoljaR1");
        List<Long> vk = (List<Long>) snapshot.get("vlasnikKoloneR1");
        Long vkon     = snapshot.getLong("vlasnikKonacnoR1");
        if (vp  != null) vlasnikPolja  = new ArrayList<>(vp);
        if (vk  != null) vlasnikKolone = new ArrayList<>(vk);
        vlasnikKonacno = vkon != null ? vkon : 0L;

        if (!RUNDA1_PAUZA.equals(status)) {
            Long timerEnd = snapshot.getLong("timerEndMsAsocijacijeR1");
            if (timerEnd != null) postaviTajmerAkoNov(timerEnd, status);
        }

        Long pk = snapshot.getLong("pokusajKolonaR1");
        pokusajKolonaCached = pk != null ? pk.intValue() : Integer.MIN_VALUE;
        pokusajTekstCached  = snapshot.getString("pokusajTekstR1");

        String pitId = snapshot.getString("pitanjeAsocijacijeIdR1");
        if (pitId != null && lokalnoPitanjeR1 == null) {
            asocRepo.dohvatiPitanjePoId(pitId, p -> {
                if (!isAdded()) return;
                lokalnoPitanjeR1 = p;
                rebuiltUIMulti(lastSeenStatus, false);
            }, err -> {});
        } else {
            rebuiltUIMulti(status, statusChanged);
        }
    }

    @SuppressWarnings("unchecked")
    private void prosesujRunda2(DocumentSnapshot snapshot, String status, boolean statusChanged) {
        jeRunda2 = true;
        List<Long> vp = (List<Long>) snapshot.get("vlasnikPoljaR2");
        List<Long> vk = (List<Long>) snapshot.get("vlasnikKoloneR2");
        Long vkon     = snapshot.getLong("vlasnikKonacnoR2");
        if (vp  != null) vlasnikPolja  = new ArrayList<>(vp);
        if (vk  != null) vlasnikKolone = new ArrayList<>(vk);
        vlasnikKonacno = vkon != null ? vkon : 0L;

        if (!RUNDA2_PAUZA.equals(status)) {
            Long timerEnd = snapshot.getLong("timerEndMsAsocijacijeR2");
            if (timerEnd != null) postaviTajmerAkoNov(timerEnd, status);
        }

        Long pk = snapshot.getLong("pokusajKolonaR2");
        pokusajKolonaCached = pk != null ? pk.intValue() : Integer.MIN_VALUE;
        pokusajTekstCached  = snapshot.getString("pokusajTekstR2");

        String pitId = snapshot.getString("pitanjeAsocijacijeIdR2");
        if (pitId != null && lokalnoPitanjeR2 == null) {
            asocRepo.dohvatiPitanjePoId(pitId, p -> {
                if (!isAdded()) return;
                lokalnoPitanjeR2 = p;
                rebuiltUIMulti(lastSeenStatus, false);
            }, err -> {});
        } else {
            rebuiltUIMulti(status, statusChanged);
        }
    }

    private void postaviTajmerAkoNov(long timerEndMs, String status) {
        if (timerEndMs == currentTimerEndMs) return;
        currentTimerEndMs = timerEndMs;
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }

        long preostalo = timerEndMs - System.currentTimeMillis();
        if (preostalo <= 0) {
            binding.tvTajmer.setText("⏱ 0:00");
            if (odrediMojuRundu(status)) zavrsiRunduMulti();
            return;
        }
        tajmer = new CountDownTimer(preostalo, 1000) {
            @Override public void onTick(long ms) {
                long sec = ms / 1000;
                binding.tvTajmer.setText(String.format("⏱ %d:%02d", sec / 60, sec % 60));
            }
            @Override public void onFinish() {
                tajmer = null;
                binding.tvTajmer.setText("⏱ 0:00");
                if (mojaRunda) zavrsiRunduMulti();
            }
        }.start();
    }

    private void rebuiltUIMulti(String status, boolean statusChanged) {
        if (!isAdded()) return;

        // PAUZA: otkrij sva polja i odbrojavaj do sledeće faze
        if (RUNDA1_PAUZA.equals(status) || RUNDA2_PAUZA.equals(status)) {
            boolean jeR1 = RUNDA1_PAUZA.equals(status);
            prikaziPauzu(
                    jeR1 ? "Priprema 2. runde" : "Kraj Asocijacija",
                    jeR1 ? "do početka 2. runde" : "do sledeće igre",
                    jeR1 ? 5 : 7,
                    () -> {
                        if (jeR1) {
                            if (jeIgrac1)
                                db.collection("partije").document(partijaId)
                                        .update("statusAsocijacije", RUNDA2_INICIJALIZACIJA);
                        } else {
                            if (jeIgrac1)
                                db.collection("partije").document(partijaId)
                                        .update("statusAsocijacije", ZAVRSENA);
                            else {
                                if (!rezultatPoslan) {
                                    rezultatPoslan = true;
                                    Bundle res = new Bundle();
                                    res.putInt("bodovi", bodovi2Multi);
                                    if (isAdded())
                                        getParentFragmentManager()
                                                .setFragmentResult("asocijacijeZavrsen", res);
                                }
                            }
                        }
                    }
            );
            return;
        }

        Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
        if (pitanje == null) return;

        // Reset dugmadi
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                itemDugmad[k][r].setTextColor(Color.parseColor("#12205A"));
                itemDugmad[k][r].setText(SLOVA[k] + (r + 1));
            }
            rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
            rezDugmad[k].setTextColor(Color.parseColor("#12205A"));
            rezDugmad[k].setText(SLOVA[k]);
        }
        binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
        binding.btnKonacno.setTextColor(Color.parseColor("#12205A"));
        binding.btnKonacno.setText("???");

        // Primijeni boje vlasnika
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                long v = vlasnikPolja.get(k * 4 + r);
                if (v != 0)
                    otkriPoljeBoja(k, r, pitanje.getPolja()[k][r],
                            v == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2);
            }
            long vk = vlasnikKolone.get(k);
            if (vk != 0)
                oznaciKolonuBoja(k, pitanje.getRjesenja()[k],
                        vk == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2);
        }
        if (vlasnikKonacno != 0)
            oznaciKonacnoBoja(pitanje.getKonacnoRjesenje(),
                    vlasnikKonacno == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2);

        // Pokušaj odgovora — vidljiv i aktivnom i čekajućem igraču
        if (pokusajKolonaCached != Integer.MIN_VALUE)
            applyPokusajVizuelno(pokusajKolonaCached, pokusajTekstCached);

        // Moj red
        boolean moj = odrediMojuRundu(status);
        if (moj && statusChanged) {
            mojePoljeOtvoreno = false;
            boolean imaZatvorenih = false;
            for (int idx = 0; idx < 16; idx++)
                if (vlasnikPolja.get(idx) == 0) { imaZatvorenih = true; break; }
            if (!imaZatvorenih) mojePoljeOtvoreno = true;
        }
        mojaRunda = moj;

        setOverlay(!moj);
        osveziDugmadMulti();
        prikaziStatusMulti(moj
                ? (jeIgrac1 ? "🔴 Tvoj red!" : "🔵 Tvoj red!")
                : (jeIgrac1 ? "🔵 Protivnik na potezu" : "🔴 Protivnik na potezu"));
    }

    private void prikaziPauzu(String naslov, String poruka, int sekunde, Runnable poslijePauze) {
        if (odbrojavanjeAktivno) return;
        odbrojavanjeAktivno = true;

        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
        mojaRunda = false;
        pokusajVSecu = false;
        osveziDugmadMulti();
        setOverlay(false);

        // Otkrij sva polja: otvorena u boji vlasnika, neotvorena sivo
        Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
        if (pitanje != null) {
            // Reset na placeholder da bi tekst polja bio vidljiv
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++) {
                    itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                    itemDugmad[k][r].setTextColor(Color.parseColor("#12205A"));
                    itemDugmad[k][r].setText(SLOVA[k] + (r + 1));
                }
                rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                rezDugmad[k].setTextColor(Color.parseColor("#12205A"));
                rezDugmad[k].setText(SLOVA[k]);
            }
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
            binding.btnKonacno.setTextColor(Color.parseColor("#12205A"));
            binding.btnKonacno.setText("???");

            // Primijeni boje: vlasnik dobija svoju boju, neotvoreno = sivo
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++) {
                    long v = vlasnikPolja.get(k * 4 + r);
                    int boja = v == 1L ? BOJA_IGRAC1 : (v == 2L ? BOJA_IGRAC2 : BOJA_NEUTRAL);
                    otkriPoljeBoja(k, r, pitanje.getPolja()[k][r], boja);
                }
                long vk = vlasnikKolone.get(k);
                int bojaK = vk == 1L ? BOJA_IGRAC1 : (vk == 2L ? BOJA_IGRAC2 : BOJA_NEUTRAL);
                oznaciKolonuBoja(k, pitanje.getRjesenja()[k], bojaK);
            }
            int bojaKon = vlasnikKonacno == 1L ? BOJA_IGRAC1
                        : (vlasnikKonacno == 2L ? BOJA_IGRAC2 : BOJA_NEUTRAL);
            oznaciKonacnoBoja(pitanje.getKonacnoRjesenje(), bojaKon);
        }

        final int[] sek = {sekunde};
        Handler h = new Handler(Looper.getMainLooper());
        Runnable tick = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                if (sek[0] > 0) {
                    prikaziStatusMulti(naslov + " — " + sek[0] + "s " + poruka);
                    sek[0]--;
                    h.postDelayed(this, 1000);
                } else {
                    odbrojavanjeAktivno = false;
                    poslijePauze.run();
                }
            }
        };
        h.post(tick);
    }

    private void osveziDugmadMulti() {
        if (pokusajVSecu || odbrojavanjeAktivno) {
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++) itemDugmad[k][r].setEnabled(false);
                rezDugmad[k].setEnabled(false);
            }
            binding.btnKonacno.setEnabled(false);
            binding.btnPredajPotez.setEnabled(false);
            return;
        }
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++)
                itemDugmad[k][r].setEnabled(mojaRunda && !mojePoljeOtvoreno
                        && vlasnikPolja.get(k * 4 + r) == 0);
            rezDugmad[k].setEnabled(mojaRunda && mojePoljeOtvoreno
                    && vlasnikKolone.get(k) == 0);
        }
        binding.btnKonacno.setEnabled(mojaRunda && mojePoljeOtvoreno && vlasnikKonacno == 0);
        binding.btnPredajPotez.setEnabled(mojaRunda);
    }

    private boolean odrediMojuRundu(String status) {
        switch (status) {
            case RUNDA1_IGRAC1_IGRA: return jeIgrac1;
            case RUNDA1_IGRAC2_IGRA: return !jeIgrac1;
            case RUNDA2_IGRAC2_IGRA: return !jeIgrac1;
            case RUNDA2_IGRAC1_IGRA: return jeIgrac1;
            default: return false;
        }
    }

    private void zavrsiRunduMulti() {
        mojaRunda = false;
        osveziDugmadMulti();
        db.collection("partije").document(partijaId)
                .update("statusAsocijacije", jeRunda2 ? RUNDA2_PAUZA : RUNDA1_PAUZA);
    }

    private void resetBoardMulti() {
        vlasnikPolja        = new ArrayList<>(Collections.nCopies(16, 0L));
        vlasnikKolone       = new ArrayList<>(Collections.nCopies(4, 0L));
        vlasnikKonacno      = 0L;
        currentTimerEndMs   = 0;
        mojePoljeOtvoreno   = false;
        odbrojavanjeAktivno = false;
        pokusajKolonaCached = Integer.MIN_VALUE;
        pokusajTekstCached  = null;
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                itemDugmad[k][r].setTextColor(Color.parseColor("#12205A"));
                itemDugmad[k][r].setText(SLOVA[k] + (r + 1));
            }
            rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
            rezDugmad[k].setTextColor(Color.parseColor("#12205A"));
            rezDugmad[k].setText(SLOVA[k]);
        }
        binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
        binding.btnKonacno.setTextColor(Color.parseColor("#12205A"));
        binding.btnKonacno.setText("???");
    }

    private int izracunajBodoveKoloneMulti(int k) {
        int neotv = 0;
        for (int r = 0; r < 4; r++) if (vlasnikPolja.get(k * 4 + r) == 0) neotv++;
        return 2 + neotv;
    }

    private int izracunajBodoveKonacnoMulti() {
        int bodi = 7;
        for (int k = 0; k < 4; k++) {
            if (vlasnikKolone.get(k) == 0) {
                boolean imaOtv = false;
                for (int r = 0; r < 4; r++)
                    if (vlasnikPolja.get(k * 4 + r) != 0) { imaOtv = true; break; }
                bodi += imaOtv ? izracunajBodoveKoloneMulti(k) : 6;
            }
        }
        return bodi;
    }

    private void otkriSvaPoljaIKolone(Asocijacija pitanje, long vlasnik) {
        int boja = vlasnik == 1L ? BOJA_IGRAC1 : BOJA_IGRAC2;
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) otkriPoljeBoja(k, r, pitanje.getPolja()[k][r], boja);
            oznaciKolonuBoja(k, pitanje.getRjesenja()[k], boja);
        }
    }

    private void setOverlay(boolean prikazi) {
        binding.overlayProtivnik.setVisibility(prikazi ? View.VISIBLE : View.GONE);
    }

    private void prikaziStatusMulti(String tekst) {
        if (binding != null) binding.tvStatus.setText(tekst);
    }

    // ─── ZAJEDNIČKI UI ────────────────────────────────────

    private void inicijalizujDugmadReference() {
        itemDugmad = new com.google.android.material.button.MaterialButton[][]{
            {binding.btnA1, binding.btnA2, binding.btnA3, binding.btnA4},
            {binding.btnB1, binding.btnB2, binding.btnB3, binding.btnB4},
            {binding.btnC1, binding.btnC2, binding.btnC3, binding.btnC4},
            {binding.btnD1, binding.btnD2, binding.btnD3, binding.btnD4}
        };
        rezDugmad = new com.google.android.material.button.MaterialButton[]{
            binding.btnRezA, binding.btnRezB, binding.btnRezC, binding.btnRezD
        };
    }

    private void otkriPoljeBoja(int k, int r, String tekst, int boja) {
        itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(boja));
        itemDugmad[k][r].setTextColor(Color.WHITE);
        itemDugmad[k][r].setText(tekst);
    }

    private void oznaciKolonuBoja(int k, String tekst, int boja) {
        rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(boja));
        rezDugmad[k].setTextColor(Color.WHITE);
        rezDugmad[k].setText(tekst);
    }

    private void oznaciKonacnoBoja(String tekst, int boja) {
        binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(boja));
        binding.btnKonacno.setTextColor(Color.WHITE);
        binding.btnKonacno.setText(tekst);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        if (listenerReg != null) listenerReg.remove();
        binding = null;
    }
}
