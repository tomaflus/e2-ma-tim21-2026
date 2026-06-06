package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentSkockoBinding;
import com.elfak.slagalica.viewModels.games.SkockoViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Random;

public class SkockoFragment extends Fragment {

    private static final long TRAJANJE_IGRAC = 30_000;
    private static final long TRAJANJE_BONUS = 10_000;

    // Statusi za multiplayer
    private static final String RUNDA1_IGRAC1_IGRA   = "RUNDA1_IGRAC1_IGRA";
    private static final String RUNDA1_IGRAC1_BONUS  = "RUNDA1_IGRAC1_BONUS";
    private static final String RUNDA2_IGRAC2_IGRA   = "RUNDA2_IGRAC2_IGRA";
    private static final String RUNDA2_IGRAC2_BONUS  = "RUNDA2_IGRAC2_BONUS";
    private static final String ZAVRSENA             = "ZAVRSENA";

    private FragmentSkockoBinding binding;

    // Solo/izazov
    private SkockoViewModel viewModel;
    private CountDownTimer tajmer;
    private Button[][] slotButtons;
    private TextView[] feedbackViews;

    // Multiplayer
    private String partijaId;
    private boolean jeIgrac1;
    private boolean jeIzazov;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;

    // Lokalni multiplayer state (bez ViewModel-a)
    private String[] tajnaKombinacija;       // kombinacija za tekucu rundu
    private String[][] tabla;                // 6x4 unosi
    private String[] feedbackovi;            // 6 feedback stringova
    private int aktivniRed = 0;
    private int aktivniSlot = 0;
    private int bodovi = 0;
    private boolean bonusRezim = false;      // da li je aktivan bonus red 7
    private int bonusSlot = 0;
    private String[] bonusPokusaj;           // unos u redu 7
    private Button[] bonusSlots;             // row 7 dugmici

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSkockoBinding.inflate(inflater, container, false);
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

        inicijalizujSlotove();

        if (jeIzazov) {
            inicijalizujSolo();
        } else {
            db = FirebaseFirestore.getInstance();
            inicijalizujMultiplayer();
        }
    }

    // ─────────────────────────────────────────────────────
    // SOLO / IZAZOV PATH  (postojeća logika, nepromenjena)
    // ─────────────────────────────────────────────────────

    private void inicijalizujSolo() {
        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);
        viewModel.score.observe(getViewLifecycleOwner(),
                s -> binding.tvBodovi.setText("Bodovi: " + s));

        viewModel.inicijalizujTajnu();
        obnavljanjeUISolo();
        obnavljanjeTajmeraSolo();

        binding.btnSimbol1.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[0]));
        binding.btnSimbol2.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[1]));
        binding.btnSimbol3.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[2]));
        binding.btnSimbol4.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[3]));
        binding.btnSimbol5.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[4]));
        binding.btnSimbol6.setOnClickListener(v -> dodajSimbolSolo(SkockoViewModel.SIMBOLI[5]));
        binding.btnPotvrdaPokusaja.setOnClickListener(v -> potvrdaPokusajaSolo());
    }

    private void obnavljanjeUISolo() {
        for (int r = 0; r < 6; r++) {
            for (int p = 0; p < 4; p++) {
                String s = viewModel.tabla[r][p];
                slotButtons[r][p].setText(s != null ? s : "");
            }
            String fb = viewModel.feedbackovi[r];
            feedbackViews[r].setText(fb != null ? fb : "");
        }
        if (viewModel.rjesenje != null) {
            binding.btnSlotR8P1.setText(viewModel.rjesenje[0]);
            binding.btnSlotR8P2.setText(viewModel.rjesenje[1]);
            binding.btnSlotR8P3.setText(viewModel.rjesenje[2]);
            binding.btnSlotR8P4.setText(viewModel.rjesenje[3]);
        }
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) {
            setSimboliEnabled(false);
            binding.btnPotvrdaPokusaja.setEnabled(false);
        }
    }

    private void obnavljanjeTajmeraSolo() {
        long sada = System.currentTimeMillis();
        switch (viewModel.faza) {
            case IGRAC:
                if (viewModel.timerEndMs == 0) {
                    viewModel.timerEndMs = sada + TRAJANJE_IGRAC;
                }
                long preostaloIgrac = viewModel.timerEndMs - sada;
                if (preostaloIgrac > 0) startTajmerIgracSolo(preostaloIgrac);
                else onTajmerIgracaIstekaaSolo();
                break;
            case PROTIVNIK:
                long preostaloProtivnik = viewModel.timerEndMs - sada;
                if (preostaloProtivnik > 0) startTajmerProtivnikSolo(preostaloProtivnik);
                else {
                    viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
                    prikaziRjesenjeSolo();
                    binding.tvTajmer.setText("Završeno");
                }
                break;
            case ZAVRSENO:
                binding.tvTajmer.setText("Završeno");
                break;
        }
    }

    private void klikNaSlotSolo(int red, int pozicija) {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        if (red != viewModel.aktivniRed) return;
        if (viewModel.aktivniSlot == 0) return;
        if (pozicija != viewModel.aktivniSlot - 1) return;
        viewModel.tabla[red][pozicija] = null;
        slotButtons[red][pozicija].setText("");
        viewModel.aktivniSlot--;
    }

    private void dodajSimbolSolo(String simbol) {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        if (viewModel.aktivniRed >= 6) return;
        if (viewModel.aktivniSlot >= 4) return;
        viewModel.tabla[viewModel.aktivniRed][viewModel.aktivniSlot] = simbol;
        slotButtons[viewModel.aktivniRed][viewModel.aktivniSlot].setText(simbol);
        viewModel.aktivniSlot++;
    }

    private void potvrdaPokusajaSolo() {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        if (viewModel.aktivniSlot < 4) {
            Toast.makeText(getContext(), "Morate izabrati 4 simbola!", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] pokusaj = viewModel.tabla[viewModel.aktivniRed].clone();
        String feedback = izracunajFeedback(pokusaj, viewModel.tajnaKombinacija);
        viewModel.feedbackovi[viewModel.aktivniRed] = feedback;
        feedbackViews[viewModel.aktivniRed].setText(feedback);

        if (feedback.equals("●●●●")) {
            int bodi = izracunajBodoveSolo();
            viewModel.score.setValue(bodi);
            viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
            if (tajmer != null) tajmer.cancel();
            prikaziRjesenjeSolo();
            return;
        }
        viewModel.aktivniRed++;
        viewModel.aktivniSlot = 0;
        if (viewModel.aktivniRed >= 6) {
            viewModel.faza = SkockoViewModel.Faza.PROTIVNIK;
            if (tajmer != null) tajmer.cancel();
            Toast.makeText(getContext(), "Niste pogodili!", Toast.LENGTH_SHORT).show();
            pokreniTajmerProtivnikaSolo();
        }
    }

    private int izracunajBodoveSolo() {
        if (viewModel.aktivniRed <= 1) return 20;
        if (viewModel.aktivniRed <= 3) return 15;
        return 10;
    }

    private void startTajmerIgracSolo(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("⏱ " + ms / 1000 + "s");
            }
            @Override public void onFinish() { onTajmerIgracaIstekaaSolo(); }
        }.start();
    }

    private void onTajmerIgracaIstekaaSolo() {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        binding.tvTajmer.setText("⏱ 0s");
        viewModel.faza = SkockoViewModel.Faza.PROTIVNIK;
        Toast.makeText(getContext(), "Isteklo vreme!", Toast.LENGTH_SHORT).show();
        pokreniTajmerProtivnikaSolo();
    }

    private void pokreniTajmerProtivnikaSolo() {
        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
        viewModel.timerEndMs = System.currentTimeMillis() + TRAJANJE_BONUS;
        startTajmerProtivnikSolo(TRAJANJE_BONUS);
    }

    private void startTajmerProtivnikSolo(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("Protivnik: " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                binding.tvTajmer.setText("Završeno");
                viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
                prikaziRjesenjeSolo();
            }
        }.start();
    }

    private void prikaziRjesenjeSolo() {
        viewModel.rjesenje = viewModel.tajnaKombinacija;
        binding.btnSlotR8P1.setText(viewModel.tajnaKombinacija[0]);
        binding.btnSlotR8P2.setText(viewModel.tajnaKombinacija[1]);
        binding.btnSlotR8P3.setText(viewModel.tajnaKombinacija[2]);
        binding.btnSlotR8P4.setText(viewModel.tajnaKombinacija[3]);

        int bod = viewModel.score.getValue() != null ? viewModel.score.getValue() : 0;
        Bundle result = new Bundle();
        result.putInt("bodovi", bod);
        if (isAdded()) getParentFragmentManager().setFragmentResult("skockoZavrsen", result);
    }

    // ─────────────────────────────────────────────────────
    // MULTIPLAYER PATH
    // ─────────────────────────────────────────────────────

    private void inicijalizujMultiplayer() {
        tabla = new String[6][4];
        feedbackovi = new String[6];
        bonusPokusaj = new String[4];
        bonusSlots = new Button[]{
            binding.btnSlotR7P1, binding.btnSlotR7P2,
            binding.btnSlotR7P3, binding.btnSlotR7P4
        };

        // symbol dugmici za multiplayer
        binding.btnSimbol1.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[0]));
        binding.btnSimbol2.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[1]));
        binding.btnSimbol3.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[2]));
        binding.btnSimbol4.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[3]));
        binding.btnSimbol5.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[4]));
        binding.btnSimbol6.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[5]));
        binding.btnPotvrdaPokusaja.setOnClickListener(v -> potvrdaPokusajaMulti());

        // Slot click za brisanje (aktivan red)
        for (int r = 0; r < 6; r++) {
            final int red = r;
            for (int p = 0; p < 4; p++) {
                final int poz = p;
                slotButtons[r][p].setOnClickListener(v -> klikNaSlotMulti(red, poz));
            }
        }
        for (int p = 0; p < 4; p++) {
            final int poz = p;
            bonusSlots[p].setOnClickListener(v -> klikNaBonusSlot(poz));
        }

        prikaziCekanje("Učitavanje...");
        slušajPartijuMulti();

        if (jeIgrac1) {
            inicijalizujRunda1KaoIgrac1();
        }
    }

    private void inicijalizujRunda1KaoIgrac1() {
        tajnaKombinacija = generisiKombinaciju();
        String tajna = String.join(",", tajnaKombinacija);
        db.collection("partije").document(partijaId).update(
                "tajnaKombinacijaR1Skocko", tajna,
                "statusSkocko", RUNDA1_IGRAC1_IGRA,
                "bodovi1Skocko", 0,
                "bodovi2Skocko", 0
        );
    }

    private void slušajPartijuMulti() {
        listenerReg = db.collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists() || !isAdded()) return;

                    String status = snapshot.getString("statusSkocko");
                    if (status == null) return;

                    Long b1L = snapshot.getLong("bodovi1Skocko");
                    Long b2L = snapshot.getLong("bodovi2Skocko");
                    int b1 = b1L != null ? b1L.intValue() : 0;
                    int b2 = b2L != null ? b2L.intValue() : 0;
                    binding.tvBodovi.setText("Bodovi: " + (jeIgrac1 ? b1 : b2));

                    switch (status) {
                        case RUNDA1_IGRAC1_IGRA:
                            if (jeIgrac1) {
                                // Aktiviran listenerom — pocni igru ako tajmer nije pokrenut
                                if (tajmer == null) pocniIgruMulti();
                            } else {
                                prikaziCekanje("Igrač 1 igra...");
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                            }
                            break;

                        case RUNDA1_IGRAC1_BONUS:
                            if (!jeIgrac1) {
                                String tajna = snapshot.getString("tajnaKombinacijaR1Skocko");
                                tajnaKombinacija = tajna != null ? tajna.split(",") : new String[4];
                                pokreniBonusRezim();
                            } else {
                                prikaziCekanje("Protivnik pokušava bonus...");
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                            }
                            break;

                        case RUNDA2_IGRAC2_IGRA:
                            resetBoardMulti();
                            aktivniRed = 0; aktivniSlot = 0;
                            bonusRezim = false;
                            if (!jeIgrac1) {
                                tajnaKombinacija = generisiKombinaciju();
                                String tajna = String.join(",", tajnaKombinacija);
                                db.collection("partije").document(partijaId).update(
                                        "tajnaKombinacijaR2Skocko", tajna
                                );
                                if (tajmer == null) pocniIgruMulti();
                            } else {
                                prikaziCekanje("Igrač 2 igra...");
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                            }
                            break;

                        case RUNDA2_IGRAC2_BONUS:
                            resetBoardMulti();
                            if (jeIgrac1) {
                                String tajna = snapshot.getString("tajnaKombinacijaR2Skocko");
                                tajnaKombinacija = tajna != null ? tajna.split(",") : new String[4];
                                pokreniBonusRezim();
                            } else {
                                prikaziCekanje("Protivnik pokušava bonus...");
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                            }
                            break;

                        case ZAVRSENA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            int mojiBodovi = jeIgrac1 ? b1 : b2;
                            Bundle result = new Bundle();
                            result.putInt("bodovi", mojiBodovi);
                            if (isAdded())
                                getParentFragmentManager().setFragmentResult("skockoZavrsen", result);
                            break;
                    }
                });
    }

    private void pocniIgruMulti() {
        setSimboliEnabled(true);
        binding.btnPotvrdaPokusaja.setEnabled(true);
        binding.tvTajmer.setText("⏱ 30s");
        // Sakrij status tekst
        tajmer = new CountDownTimer(TRAJANJE_IGRAC, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("⏱ " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                tajmer = null;
                onTajmerIstekaoMulti();
            }
        }.start();
    }

    private void onTajmerIstekaoMulti() {
        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
        binding.tvTajmer.setText("⏱ 0s");
        String sledeci = jeIgrac1 ? RUNDA1_IGRAC1_BONUS : RUNDA2_IGRAC2_BONUS;
        db.collection("partije").document(partijaId)
                .update("statusSkocko", sledeci);
    }

    private void dodajSimbolMulti(String simbol) {
        if (bonusRezim) {
            if (bonusSlot >= 4) return;
            bonusPokusaj[bonusSlot] = simbol;
            bonusSlots[bonusSlot].setText(simbol);
            bonusSlot++;
            return;
        }
        if (aktivniRed >= 6 || aktivniSlot >= 4) return;
        tabla[aktivniRed][aktivniSlot] = simbol;
        slotButtons[aktivniRed][aktivniSlot].setText(simbol);
        aktivniSlot++;
    }

    private void klikNaSlotMulti(int red, int pozicija) {
        if (bonusRezim || red != aktivniRed || aktivniSlot == 0) return;
        if (pozicija != aktivniSlot - 1) return;
        tabla[red][pozicija] = null;
        slotButtons[red][pozicija].setText("");
        aktivniSlot--;
    }

    private void klikNaBonusSlot(int pozicija) {
        if (!bonusRezim || pozicija != bonusSlot - 1 || bonusSlot == 0) return;
        bonusPokusaj[pozicija] = null;
        bonusSlots[pozicija].setText("");
        bonusSlot--;
    }

    private void potvrdaPokusajaMulti() {
        if (bonusRezim) {
            if (bonusSlot < 4) {
                Toast.makeText(getContext(), "Izaberite 4 simbola!", Toast.LENGTH_SHORT).show();
                return;
            }
            provjeriBonus();
            return;
        }

        if (aktivniSlot < 4) {
            Toast.makeText(getContext(), "Izaberite 4 simbola!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] pokusaj = tabla[aktivniRed].clone();
        String feedback = izracunajFeedback(pokusaj, tajnaKombinacija);
        feedbackovi[aktivniRed] = feedback;
        feedbackViews[aktivniRed].setText(feedback);

        if (feedback.equals("●●●●")) {
            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
            int bodi = izracunajBodoveMulti();
            bodovi += bodi;
            String poljeB = jeIgrac1 ? "bodovi1Skocko" : "bodovi2Skocko";
            binding.btnSlotR8P1.setText(tajnaKombinacija[0]);
            binding.btnSlotR8P2.setText(tajnaKombinacija[1]);
            binding.btnSlotR8P3.setText(tajnaKombinacija[2]);
            binding.btnSlotR8P4.setText(tajnaKombinacija[3]);
            setSimboliEnabled(false);
            binding.btnPotvrdaPokusaja.setEnabled(false);
            String sledeci = jeIgrac1 ? RUNDA2_IGRAC2_IGRA : ZAVRSENA;
            db.collection("partije").document(partijaId).update(poljeB, bodovi,
                    "statusSkocko", sledeci);
            return;
        }

        aktivniRed++;
        aktivniSlot = 0;

        if (aktivniRed >= 6) {
            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
            setSimboliEnabled(false);
            binding.btnPotvrdaPokusaja.setEnabled(false);
            binding.btnSlotR8P1.setText(tajnaKombinacija[0]);
            binding.btnSlotR8P2.setText(tajnaKombinacija[1]);
            binding.btnSlotR8P3.setText(tajnaKombinacija[2]);
            binding.btnSlotR8P4.setText(tajnaKombinacija[3]);
            String sledeci = jeIgrac1 ? RUNDA1_IGRAC1_BONUS : RUNDA2_IGRAC2_BONUS;
            db.collection("partije").document(partijaId)
                    .update("statusSkocko", sledeci);
        }
    }

    private void provjeriBonus() {
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
        String feedback = izracunajFeedback(bonusPokusaj, tajnaKombinacija);
        binding.tvFeedbackR7.setText(feedback);

        boolean pogodio = feedback.equals("●●●●");
        String poljeB = jeIgrac1 ? "bodovi1Skocko" : "bodovi2Skocko";
        if (pogodio) bodovi += 10;

        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
        for (Button b : bonusSlots) b.setEnabled(false);

        boolean bilR1 = jeIgrac1 ? false : true; // igrac1 bonus = R1 bonus
        // Igrac2 ima bonus u R1 → sledeci je R2_IGRAC2_IGRA
        // Igrac1 ima bonus u R2 → sledeci je ZAVRSENA
        String sledeci;
        if (!jeIgrac1) {
            // Igrac2 je bio na bonusu R1
            sledeci = RUNDA2_IGRAC2_IGRA;
        } else {
            // Igrac1 je bio na bonusu R2
            sledeci = ZAVRSENA;
        }

        db.collection("partije").document(partijaId).update(
                poljeB, bodovi,
                "statusSkocko", sledeci
        );
    }

    private void pokreniBonusRezim() {
        bonusRezim = true;
        bonusSlot = 0;
        bonusPokusaj = new String[4];
        for (Button b : bonusSlots) {
            b.setText("");
            b.setEnabled(true);
        }
        setSimboliEnabled(true);
        binding.btnPotvrdaPokusaja.setEnabled(true);
        if (tajmer != null) tajmer.cancel();
        tajmer = new CountDownTimer(TRAJANJE_BONUS, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("Bonus: " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                tajmer = null;
                // Istekao bonus — bez pogotka
                bonusSlot = 0;
                for (String s : bonusPokusaj) if (s == null) {
                    // Nije unio 4 simbola — preci dalje
                    setSimboliEnabled(false);
                    binding.btnPotvrdaPokusaja.setEnabled(false);
                    for (Button b : bonusSlots) b.setEnabled(false);
                    String sledeci = !jeIgrac1 ? RUNDA2_IGRAC2_IGRA : ZAVRSENA;
                    db.collection("partije").document(partijaId)
                            .update("statusSkocko", sledeci);
                    return;
                }
                // Unio je 4 — provjeri
                provjeriBonus();
            }
        }.start();
    }

    private int izracunajBodoveMulti() {
        if (aktivniRed <= 1) return 20;
        if (aktivniRed <= 3) return 15;
        return 10;
    }

    private void resetBoardMulti() {
        tabla = new String[6][4];
        feedbackovi = new String[6];
        for (int r = 0; r < 6; r++) {
            for (int p = 0; p < 4; p++) slotButtons[r][p].setText("");
            feedbackViews[r].setText("");
        }
        bonusRezim = false;
        bonusSlot = 0;
        bonusPokusaj = new String[4];
        for (Button b : bonusSlots) { b.setText(""); b.setEnabled(false); }
        binding.tvFeedbackR7.setText("");
        binding.btnSlotR8P1.setText("");
        binding.btnSlotR8P2.setText("");
        binding.btnSlotR8P3.setText("");
        binding.btnSlotR8P4.setText("");
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
    }

    private void prikaziCekanje(String poruka) {
        binding.tvTajmer.setText(poruka);
        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
    }

    // ─────────────────────────────────────────────────────
    // ZAJEDNICKI UTILITY
    // ─────────────────────────────────────────────────────

    private void inicijalizujSlotove() {
        slotButtons = new Button[][]{
            {binding.btnSlotR1P1, binding.btnSlotR1P2, binding.btnSlotR1P3, binding.btnSlotR1P4},
            {binding.btnSlotR2P1, binding.btnSlotR2P2, binding.btnSlotR2P3, binding.btnSlotR2P4},
            {binding.btnSlotR3P1, binding.btnSlotR3P2, binding.btnSlotR3P3, binding.btnSlotR3P4},
            {binding.btnSlotR4P1, binding.btnSlotR4P2, binding.btnSlotR4P3, binding.btnSlotR4P4},
            {binding.btnSlotR5P1, binding.btnSlotR5P2, binding.btnSlotR5P3, binding.btnSlotR5P4},
            {binding.btnSlotR6P1, binding.btnSlotR6P2, binding.btnSlotR6P3, binding.btnSlotR6P4},
        };
        feedbackViews = new TextView[]{
            binding.tvFeedbackR1, binding.tvFeedbackR2, binding.tvFeedbackR3,
            binding.tvFeedbackR4, binding.tvFeedbackR5, binding.tvFeedbackR6
        };

        // Slot click listeneri za solo — multiplayer ih prepisuje
        for (int r = 0; r < 6; r++) {
            final int red = r;
            for (int p = 0; p < 4; p++) {
                final int pozicija = p;
                slotButtons[r][p].setOnClickListener(v -> klikNaSlotSolo(red, pozicija));
            }
        }
    }

    private String izracunajFeedback(String[] pokusaj, String[] tajna) {
        boolean[] oznacenPokusaj = new boolean[4];
        boolean[] oznacenaTajna = new boolean[4];
        int tacno = 0, djelimicno = 0;
        for (int i = 0; i < 4; i++) {
            if (pokusaj[i] != null && pokusaj[i].equals(tajna[i])) {
                tacno++;
                oznacenPokusaj[i] = oznacenaTajna[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (oznacenPokusaj[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (!oznacenaTajna[j] && pokusaj[i] != null && pokusaj[i].equals(tajna[j])) {
                    djelimicno++;
                    oznacenaTajna[j] = true;
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tacno; i++) sb.append("●");
        for (int i = 0; i < djelimicno; i++) sb.append("○");
        return sb.toString();
    }

    private String[] generisiKombinaciju() {
        String[] kombinacija = new String[4];
        Random rnd = new Random();
        for (int i = 0; i < 4; i++) {
            kombinacija[i] = SkockoViewModel.SIMBOLI[rnd.nextInt(SkockoViewModel.SIMBOLI.length)];
        }
        return kombinacija;
    }

    private void setSimboliEnabled(boolean enabled) {
        binding.btnSimbol1.setEnabled(enabled);
        binding.btnSimbol2.setEnabled(enabled);
        binding.btnSimbol3.setEnabled(enabled);
        binding.btnSimbol4.setEnabled(enabled);
        binding.btnSimbol5.setEnabled(enabled);
        binding.btnSimbol6.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        if (listenerReg != null) listenerReg.remove();
        binding = null;
    }
}
