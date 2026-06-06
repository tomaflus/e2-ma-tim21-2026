package com.elfak.slagalica.fragments.games;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsocijacijeFragment extends Fragment {

    private static final long TRAJANJE_RUNDE = 120_000;
    private static final int BOJA_SKRIVENO  = Color.parseColor("#A8D0EC");
    private static final int BOJA_OTKRIVENO = Color.parseColor("#C0392B");

    // Multiplayer statusi
    private static final String RUNDA1_IGRAC1_IGRA     = "RUNDA1_IGRAC1_IGRA";
    private static final String RUNDA1_IGRAC2_IGRA     = "RUNDA1_IGRAC2_IGRA";
    private static final String RUNDA2_INICIJALIZACIJA = "RUNDA2_INICIJALIZACIJA";
    private static final String RUNDA2_IGRAC2_IGRA     = "RUNDA2_IGRAC2_IGRA";
    private static final String RUNDA2_IGRAC1_IGRA     = "RUNDA2_IGRAC1_IGRA";
    private static final String ZAVRSENA               = "ZAVRSENA";

    private FragmentAsocijacijeBinding binding;

    // Args
    private String partijaId;
    private boolean jeIgrac1;
    private boolean jeIzazov;

    // Zajednička referenca na dugmad
    private com.google.android.material.button.MaterialButton[][] itemDugmad;
    private com.google.android.material.button.MaterialButton[] rezDugmad;

    // ── Solo (jeIzazov=true) ──────────────────────────────
    private AsocijacijeViewModel viewModel;
    private CountDownTimer tajmer;

    // ── Multiplayer (jeIzazov=false) ─────────────────────
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private AsocijacijeRepository asocRepo;
    private Asocijacija lokalnoPitanjeR1;
    private Asocijacija lokalnoPitanjeR2;
    private boolean jeRunda2    = false;
    private boolean mojaRunda   = false;
    private List<Boolean> otvorenaPolja;   // 16 elemenata (kolona*4+red)
    private List<Boolean> pogodeneKolone;  // 4 elementa
    private boolean konacnoPogodeno = false;
    private int bodovi1Multi = 0;
    private int bodovi2Multi = 0;
    private long currentTimerEndMs = 0;    // sprječava restart tajmera na svakom snapshot-u

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
            partijaId = getArguments().getString("partijaId");
            jeIgrac1  = getArguments().getBoolean("jeIgrac1", true);
            jeIzazov  = getArguments().getBoolean("jeIzazov", false);
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

    // ═════════════════════════════════════════════════════
    // SOLO / IZAZOV PATH
    // ═════════════════════════════════════════════════════

    private void inicijalizujSolo() {
        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);
        viewModel.score.observe(getViewLifecycleOwner(),
                s -> binding.tvBodovi.setText("Bodovi: " + s));
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
                        otkriPolje(kolona, red, AsocijacijeViewModel.POLJA[kolona][red]);
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
                    otkriPolje(k, r, AsocijacijeViewModel.POLJA[k][r]);
            if (viewModel.pogodenaKolona[k])
                oznaciPogodenuKolonu(k, AsocijacijeViewModel.RJESENJA_KOLONA[k]);
        }
        if (viewModel.konacnoPogodeno) {
            binding.btnKonacno.setText(AsocijacijeViewModel.KONACNO_RJESENJE);
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);
        }
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
            int bodi = izracunajBodoveKonacnoSolo();
            dodajBodoveSolo(bodi);
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++)
                    if (!viewModel.otvorenaPolja[k][r]) {
                        viewModel.otvorenaPolja[k][r] = true;
                        otkriPolje(k, r, AsocijacijeViewModel.POLJA[k][r]);
                    }
                if (!viewModel.pogodenaKolona[k]) {
                    viewModel.pogodenaKolona[k] = true;
                    oznaciPogodenuKolonu(k, AsocijacijeViewModel.RJESENJA_KOLONA[k]);
                }
            }
            binding.btnKonacno.setText(AsocijacijeViewModel.KONACNO_RJESENJE);
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);
            posaljiRezultatSolo();
        } else {
            viewModel.pogodenaKolona[kolona] = true;
            int bodi = izracunajBodoveKoloneSolo(kolona);
            dodajBodoveSolo(bodi);
            for (int r = 0; r < 4; r++)
                if (!viewModel.otvorenaPolja[kolona][r]) {
                    viewModel.otvorenaPolja[kolona][r] = true;
                    otkriPolje(kolona, r, AsocijacijeViewModel.POLJA[kolona][r]);
                }
            oznaciPogodenuKolonu(kolona, AsocijacijeViewModel.RJESENJA_KOLONA[kolona]);
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

    // ═════════════════════════════════════════════════════
    // MULTIPLAYER PATH
    // ═════════════════════════════════════════════════════

    private void inicijalizujMultiplayer() {
        otvorenaPolja  = new ArrayList<>(Collections.nCopies(16, false));
        pogodeneKolone = new ArrayList<>(Collections.nCopies(4, false));

        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.btnPredajPotez.setVisibility(View.VISIBLE);
        binding.btnPredajPotez.setOnClickListener(v -> predajPotez());

        postaviListenereMultiplayer();
        onemogucuiInterakcijuMulti(false);
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
                    "pitanjeAsocijacijeIdR1", pitanje.getId(),
                    "otvorenaPoljaR1",         new ArrayList<>(Collections.nCopies(16, false)),
                    "pogodeneKoloneR1",        new ArrayList<>(Collections.nCopies(4, false)),
                    "konacnoPogodenoR1",       false,
                    "bodovi1Asocijacije",      0,
                    "bodovi2Asocijacije",      0,
                    "timerEndMsAsocijacijeR1", timerEnd,
                    "statusAsocijacije",       RUNDA1_IGRAC1_IGRA
            );
        }, err -> Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show());
    }

    private void inicijalizujRunda2KaoIgrac2() {
        asocRepo.dohvatiNasumicnoPitanje(pitanje -> {
            if (!isAdded()) return;
            lokalnoPitanjeR2 = pitanje;
            long timerEnd = System.currentTimeMillis() + TRAJANJE_RUNDE;
            db.collection("partije").document(partijaId).update(
                    "pitanjeAsocijacijeIdR2", pitanje.getId(),
                    "otvorenaPoljaR2",         new ArrayList<>(Collections.nCopies(16, false)),
                    "pogodeneKoloneR2",        new ArrayList<>(Collections.nCopies(4, false)),
                    "konacnoPogodenoR2",       false,
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
                    if (!mojaRunda || otvorenaPolja.get(idx)) return;
                    Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
                    if (pitanje == null) return;
                    // Optimistično ažuriranje UI
                    otvorenaPolja.set(idx, true);
                    otkriPolje(kolona, red, pitanje.getPolja()[kolona][red]);
                    itemDugmad[kolona][red].setEnabled(false);
                    String poljeF = jeRunda2 ? "otvorenaPoljaR2" : "otvorenaPoljaR1";
                    db.collection("partije").document(partijaId)
                            .update(poljeF, new ArrayList<>(otvorenaPolja));
                });
            }
            final int kolona = k;
            rezDugmad[k].setOnClickListener(v -> {
                if (!mojaRunda || pogodeneKolone.get(kolona)) return;
                Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
                if (pitanje != null) prikaziDialogMulti(kolona, pitanje);
            });
        }
        binding.btnKonacno.setOnClickListener(v -> {
            if (!mojaRunda || konacnoPogodeno) return;
            Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
            if (pitanje != null) prikaziDialogMulti(-1, pitanje);
        });
    }

    private void prikaziDialogMulti(int kolona, Asocijacija pitanje) {
        String naslov = kolona == -1 ? "Konačno rešenje"
                : "Rešenje kolone " + new String[]{"A", "B", "C", "D"}[kolona];
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
            Toast.makeText(getContext(), "Netačno!", Toast.LENGTH_SHORT).show();
            predajPotez();
            return;
        }

        if (kolona == -1) {
            // Pogodio konačno — završi rundu
            konacnoPogodeno = true;
            int bodi        = izracunajBodoveKonacnoMulti();
            String poljeB   = jeIgrac1 ? "bodovi1Asocijacije" : "bodovi2Asocijacije";
            int noviB       = (jeIgrac1 ? bodovi1Multi : bodovi2Multi) + bodi;
            String poljeKonacno = jeRunda2 ? "konacnoPogodenoR2" : "konacnoPogodenoR1";
            String poljeOtv = jeRunda2 ? "otvorenaPoljaR2"    : "otvorenaPoljaR1";
            String poljeKol = jeRunda2 ? "pogodeneKoloneR2"   : "pogodeneKoloneR1";
            String sledeci  = jeRunda2 ? ZAVRSENA : RUNDA2_INICIJALIZACIJA;

            otkriSvaPoljaIKolone(pitanje);
            binding.btnKonacno.setText(pitanje.getKonacnoRjesenje());
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);

            db.collection("partije").document(partijaId).update(
                    poljeB,       noviB,
                    poljeKonacno, true,
                    poljeOtv,     new ArrayList<>(Collections.nCopies(16, true)),
                    poljeKol,     new ArrayList<>(Collections.nCopies(4, true)),
                    "statusAsocijacije", sledeci
            );
        } else {
            // Pogodio kolonu — nastavi (ne mijenjaj status)
            pogodeneKolone.set(kolona, true);
            int bodi    = izracunajBodoveKoloneMulti(kolona);
            String poljeB = jeIgrac1 ? "bodovi1Asocijacije" : "bodovi2Asocijacije";
            int noviB   = (jeIgrac1 ? bodovi1Multi : bodovi2Multi) + bodi;
            if (jeIgrac1) bodovi1Multi = noviB; else bodovi2Multi = noviB;
            binding.tvBodovi.setText("Bodovi: " + noviB);

            for (int r = 0; r < 4; r++) otvorenaPolja.set(kolona * 4 + r, true);
            for (int r = 0; r < 4; r++) otkriPolje(kolona, r, pitanje.getPolja()[kolona][r]);
            oznaciPogodenuKolonu(kolona, pitanje.getRjesenja()[kolona]);
            rezDugmad[kolona].setEnabled(false);

            String poljeOtv = jeRunda2 ? "otvorenaPoljaR2" : "otvorenaPoljaR1";
            String poljeKol = jeRunda2 ? "pogodeneKoloneR2" : "pogodeneKoloneR1";
            db.collection("partije").document(partijaId).update(
                    poljeB,   noviB,
                    poljeOtv, new ArrayList<>(otvorenaPolja),
                    poljeKol, new ArrayList<>(pogodeneKolone)
            );
            Toast.makeText(getContext(), "Tačno! +" + bodi, Toast.LENGTH_SHORT).show();
        }
    }

    private void predajPotez() {
        String sledeci = sledeciStatus();
        if (sledeci == null) return;
        db.collection("partije").document(partijaId)
                .update("statusAsocijacije", sledeci);
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

                    Long b1L = snapshot.getLong("bodovi1Asocijacije");
                    Long b2L = snapshot.getLong("bodovi2Asocijacije");
                    bodovi1Multi = b1L != null ? b1L.intValue() : 0;
                    bodovi2Multi = b2L != null ? b2L.intValue() : 0;
                    binding.tvBodovi.setText("Bodovi: " + (jeIgrac1 ? bodovi1Multi : bodovi2Multi));

                    switch (status) {
                        case RUNDA1_IGRAC1_IGRA:
                        case RUNDA1_IGRAC2_IGRA: {
                            jeRunda2 = false;
                            String pitId = snapshot.getString("pitanjeAsocijacijeIdR1");
                            List<Boolean> otvR1 = (List<Boolean>) snapshot.get("otvorenaPoljaR1");
                            List<Boolean> kol1  = (List<Boolean>) snapshot.get("pogodeneKoloneR1");
                            Boolean konR1 = snapshot.getBoolean("konacnoPogodenoR1");
                            if (otvR1 != null) otvorenaPolja  = new ArrayList<>(otvR1);
                            if (kol1  != null) pogodeneKolone = new ArrayList<>(kol1);
                            konacnoPogodeno = Boolean.TRUE.equals(konR1);

                            Long timerEnd = snapshot.getLong("timerEndMsAsocijacijeR1");
                            if (timerEnd != null) postaviTajmerAkoNov(timerEnd, status);

                            if (pitId != null && lokalnoPitanjeR1 == null) {
                                asocRepo.dohvatiPitanjePoId(pitId, p -> {
                                    if (!isAdded()) return;
                                    lokalnoPitanjeR1 = p;
                                    rebuiltUIMulti(status);
                                }, err -> {});
                            } else {
                                rebuiltUIMulti(status);
                            }
                            break;
                        }

                        case RUNDA2_INICIJALIZACIJA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            prikaziStatusMulti("Priprema runde 2...");
                            onemogucuiInterakcijuMulti(false);
                            if (!jeIgrac1) {
                                resetBoardMulti();
                                inicijalizujRunda2KaoIgrac2();
                            }
                            break;

                        case RUNDA2_IGRAC2_IGRA:
                        case RUNDA2_IGRAC1_IGRA: {
                            jeRunda2 = true;
                            String pitId = snapshot.getString("pitanjeAsocijacijeIdR2");
                            List<Boolean> otvR2 = (List<Boolean>) snapshot.get("otvorenaPoljaR2");
                            List<Boolean> kol2  = (List<Boolean>) snapshot.get("pogodeneKoloneR2");
                            Boolean konR2 = snapshot.getBoolean("konacnoPogodenoR2");
                            if (otvR2 != null) otvorenaPolja  = new ArrayList<>(otvR2);
                            if (kol2  != null) pogodeneKolone = new ArrayList<>(kol2);
                            konacnoPogodeno = Boolean.TRUE.equals(konR2);

                            Long timerEnd = snapshot.getLong("timerEndMsAsocijacijeR2");
                            if (timerEnd != null) postaviTajmerAkoNov(timerEnd, status);

                            if (pitId != null && lokalnoPitanjeR2 == null) {
                                asocRepo.dohvatiPitanjePoId(pitId, p -> {
                                    if (!isAdded()) return;
                                    lokalnoPitanjeR2 = p;
                                    rebuiltUIMulti(status);
                                }, err -> {});
                            } else {
                                rebuiltUIMulti(status);
                            }
                            break;
                        }

                        case ZAVRSENA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            onemogucuiInterakcijuMulti(false);
                            prikaziStatusMulti("Igra završena!");
                            int mojiBodovi = jeIgrac1 ? bodovi1Multi : bodovi2Multi;
                            Bundle result = new Bundle();
                            result.putInt("bodovi", mojiBodovi);
                            if (isAdded())
                                getParentFragmentManager()
                                        .setFragmentResult("asocijacijeZavrsen", result);
                            break;
                    }
                });
    }

    // Restartuje tajmer samo ako je timerEndMs nov (sprječava reset na svakom snapshot-u)
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

    private void rebuiltUIMulti(String status) {
        if (!isAdded()) return;
        Asocijacija pitanje = jeRunda2 ? lokalnoPitanjeR2 : lokalnoPitanjeR1;
        if (pitanje == null) return;

        // Reset vizuelnog stanja
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
                itemDugmad[k][r].setTextColor(Color.parseColor("#12205A"));
                itemDugmad[k][r].setText(new String[]{"A", "B", "C", "D"}[k] + (r + 1));
            }
            rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
            rezDugmad[k].setTextColor(Color.parseColor("#12205A"));
            rezDugmad[k].setText(new String[]{"A", "B", "C", "D"}[k]);
        }
        binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_SKRIVENO));
        binding.btnKonacno.setTextColor(Color.parseColor("#12205A"));
        binding.btnKonacno.setText("???");

        // Primjeni trenutno stanje
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++)
                if (otvorenaPolja.get(k * 4 + r))
                    otkriPolje(k, r, pitanje.getPolja()[k][r]);
            if (pogodeneKolone.get(k))
                oznaciPogodenuKolonu(k, pitanje.getRjesenja()[k]);
        }
        if (konacnoPogodeno) {
            binding.btnKonacno.setText(pitanje.getKonacnoRjesenje());
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);
        }

        boolean moj = odrediMojuRundu(status);
        onemogucuiInterakcijuMulti(moj);
        prikaziStatusMulti(moj ? "Tvoj red!" : "Red protivnika...");
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

    private void onemogucuiInterakcijuMulti(boolean ukljuci) {
        mojaRunda = ukljuci;
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                boolean poljeOtvoreno = otvorenaPolja != null && otvorenaPolja.get(k * 4 + r);
                itemDugmad[k][r].setEnabled(ukljuci && !poljeOtvoreno);
            }
            boolean kolonaPogodjene = pogodeneKolone != null && pogodeneKolone.get(k);
            rezDugmad[k].setEnabled(ukljuci && !kolonaPogodjene);
        }
        binding.btnKonacno.setEnabled(ukljuci && !konacnoPogodeno);
        binding.btnPredajPotez.setEnabled(ukljuci);
    }

    private void zavrsiRunduMulti() {
        onemogucuiInterakcijuMulti(false);
        String sledeci = jeRunda2 ? ZAVRSENA : RUNDA2_INICIJALIZACIJA;
        db.collection("partije").document(partijaId)
                .update("statusAsocijacije", sledeci);
    }

    private void resetBoardMulti() {
        otvorenaPolja     = new ArrayList<>(Collections.nCopies(16, false));
        pogodeneKolone    = new ArrayList<>(Collections.nCopies(4, false));
        konacnoPogodeno   = false;
        currentTimerEndMs = 0;
    }

    private int izracunajBodoveKoloneMulti(int k) {
        int neotv = 0;
        for (int r = 0; r < 4; r++) if (!otvorenaPolja.get(k * 4 + r)) neotv++;
        return 2 + neotv;
    }

    private int izracunajBodoveKonacnoMulti() {
        int bodi = 7;
        for (int k = 0; k < 4; k++) {
            if (!pogodeneKolone.get(k)) {
                boolean imaOtv = false;
                for (int r = 0; r < 4; r++) if (otvorenaPolja.get(k * 4 + r)) { imaOtv = true; break; }
                bodi += imaOtv ? izracunajBodoveKoloneMulti(k) : 6;
            }
        }
        return bodi;
    }

    private void otkriSvaPoljaIKolone(Asocijacija pitanje) {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) otkriPolje(k, r, pitanje.getPolja()[k][r]);
            oznaciPogodenuKolonu(k, pitanje.getRjesenja()[k]);
        }
    }

    private void prikaziStatusMulti(String tekst) {
        if (binding != null) binding.tvStatus.setText(tekst);
    }

    // ═════════════════════════════════════════════════════
    // ZAJEDNIČKI UI UTILITY
    // ═════════════════════════════════════════════════════

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

    private void otkriPolje(int k, int r, String tekst) {
        itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
        itemDugmad[k][r].setTextColor(Color.WHITE);
        itemDugmad[k][r].setText(tekst);
    }

    private void oznaciPogodenuKolonu(int k, String tekst) {
        rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
        rezDugmad[k].setTextColor(Color.WHITE);
        rezDugmad[k].setText(tekst);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        if (listenerReg != null) listenerReg.remove();
        binding = null;
    }
}
