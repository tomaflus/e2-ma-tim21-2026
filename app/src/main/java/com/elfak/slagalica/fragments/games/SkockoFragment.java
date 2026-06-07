package com.elfak.slagalica.fragments.games;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentSkockoBinding;
import com.elfak.slagalica.viewModels.games.SkockoViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Random;

public class SkockoFragment extends Fragment {

    private static final long TRAJANJE_IGRAC = 30_000;
    private static final long TRAJANJE_BONUS = 10_000;

    private static final String RUNDA1_IGRAC1_IGRA   = "RUNDA1_IGRAC1_IGRA";
    private static final String RUNDA1_IGRAC1_BONUS  = "RUNDA1_IGRAC1_BONUS";
    private static final String RUNDA1_PAUZA         = "RUNDA1_PAUZA";
    private static final String RUNDA2_IGRAC2_IGRA   = "RUNDA2_IGRAC2_IGRA";
    private static final String RUNDA2_IGRAC2_BONUS  = "RUNDA2_IGRAC2_BONUS";
    private static final String RUNDA2_PAUZA         = "RUNDA2_PAUZA";
    private static final String ZAVRSENA             = "ZAVRSENA";

    // Boje feedbacka
    private static final int BOJA_TACNO    = Color.parseColor("#CC0000"); // crvena
    private static final int BOJA_POSTOJI  = Color.parseColor("#FFD700"); // žuta
    private static final int BOJA_NEMA     = Color.parseColor("#1A1A1A"); // crna

    private FragmentSkockoBinding binding;

    // Solo/izazov
    private SkockoViewModel viewModel;
    private CountDownTimer tajmer;
    private Button[][] slotButtons;

    // Feedback krugovi: [7 redova][4 kruga], redovi 0-5 su pokušaji, red 6 je bonus
    private LinearLayout[] feedbackContainers;
    private View[][] feedbackCircles;

    // Multiplayer
    private String partijaId;
    private boolean jeIgrac1;
    private boolean jeIzazov;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;

    private String[] tajnaKombinacija;
    private String[][] tabla;
    private String[] feedbackovi;
    private int aktivniRed = 0;
    private int aktivniSlot = 0;
    private int bodovi = 0;
    private boolean bonusRezim = false;
    private int bonusSlot = 0;
    private String[] bonusPokusaj;
    private Button[] bonusSlots;

    private String lastSeenStatus = null;
    private boolean odbrojavanjeAktivno = false;
    private int[][] feedbackBrojevi = new int[6][2];

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
        inicijalizujFeedbackKrugove();

        if (jeIzazov) {
            inicijalizujSolo();
        } else {
            db = FirebaseFirestore.getInstance();
            inicijalizujMultiplayer();
        }
    }

    // ─────────────────────────────────────────────────────
    // FEEDBACK KRUGOVI
    // ─────────────────────────────────────────────────────

    private void inicijalizujFeedbackKrugove() {
        feedbackContainers = new LinearLayout[]{
            binding.llFeedbackR1, binding.llFeedbackR2, binding.llFeedbackR3,
            binding.llFeedbackR4, binding.llFeedbackR5, binding.llFeedbackR6,
            binding.llFeedbackR7
        };
        feedbackCircles = new View[7][4];
        int size   = dpToPx(22);
        int margin = dpToPx(2);

        for (int row = 0; row < 7; row++) {
            LinearLayout container = feedbackContainers[row];
            container.removeAllViews();

            LinearLayout gornji = new LinearLayout(requireContext());
            gornji.setGravity(Gravity.CENTER);
            LinearLayout donji  = new LinearLayout(requireContext());
            donji.setGravity(Gravity.CENTER);

            for (int i = 0; i < 4; i++) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(BOJA_NEMA);

                View circle = new View(requireContext());
                circle.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(margin, margin, margin, margin);
                circle.setLayoutParams(lp);

                feedbackCircles[row][i] = circle;
                (i < 2 ? gornji : donji).addView(circle);
            }
            container.addView(gornji);
            container.addView(donji);
        }
    }

    private void setFeedbackKrugovi(int red, int tacno, int djelimicno) {
        if (feedbackCircles == null || red < 0 || red >= feedbackCircles.length) return;
        for (int i = 0; i < 4; i++) {
            int boja;
            if (i < tacno)               boja = BOJA_TACNO;
            else if (i < tacno + djelimicno) boja = BOJA_POSTOJI;
            else                          boja = BOJA_NEMA;
            ((GradientDrawable) feedbackCircles[red][i].getBackground()).setColor(boja);
        }
    }

    private int[] izracunajFeedbackBrojevi(String[] pokusaj, String[] tajna) {
        boolean[] oznacenP = new boolean[4];
        boolean[] oznacenT = new boolean[4];
        int tacno = 0, djel = 0;
        for (int i = 0; i < 4; i++) {
            if (pokusaj[i] != null && pokusaj[i].equals(tajna[i])) {
                tacno++;
                oznacenP[i] = oznacenT[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (oznacenP[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (!oznacenT[j] && pokusaj[i] != null && pokusaj[i].equals(tajna[j])) {
                    djel++;
                    oznacenT[j] = true;
                    break;
                }
            }
        }
        return new int[]{tacno, djel};
    }

    // ─────────────────────────────────────────────────────
    // SOLO / IZAZOV PATH
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
            if (fb != null) {
                int[] nums = parseFeedback(fb);
                setFeedbackKrugovi(r, nums[0], nums[1]);
            }
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
                if (viewModel.timerEndMs == 0) viewModel.timerEndMs = sada + TRAJANJE_IGRAC;
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
        int[] fb = izracunajFeedbackBrojevi(pokusaj, viewModel.tajnaKombinacija);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fb[0]; i++) sb.append("●");
        for (int i = 0; i < fb[1]; i++) sb.append("○");
        viewModel.feedbackovi[viewModel.aktivniRed] = sb.toString();
        setFeedbackKrugovi(viewModel.aktivniRed, fb[0], fb[1]);

        if (fb[0] == 4) {
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

        binding.tvStatus.setVisibility(View.VISIBLE);
        prikaziStatusMulti("Učitavanje...");
        setOverlay(true);

        binding.btnSimbol1.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[0]));
        binding.btnSimbol2.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[1]));
        binding.btnSimbol3.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[2]));
        binding.btnSimbol4.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[3]));
        binding.btnSimbol5.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[4]));
        binding.btnSimbol6.setOnClickListener(v -> dodajSimbolMulti(SkockoViewModel.SIMBOLI[5]));
        binding.btnPotvrdaPokusaja.setOnClickListener(v -> potvrdaPokusajaMulti());

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

        slušajPartijuMulti();
        if (jeIgrac1) inicijalizujRunda1KaoIgrac1();
    }

    private void inicijalizujRunda1KaoIgrac1() {
        tajnaKombinacija = generisiKombinaciju();
        String tajna = String.join(",", tajnaKombinacija);
        lastSeenStatus = RUNDA1_IGRAC1_IGRA;
        db.collection("partije").document(partijaId).update(
                "tajnaKombinacijaR1Skocko", tajna,
                "statusSkocko", RUNDA1_IGRAC1_IGRA,
                "bodovi1Skocko", 0,
                "bodovi2Skocko", 0
        );
        setOverlay(false);
        prikaziStatusMulti("🔴 Tvoj red!");
        pocniIgruMulti();
    }

    private void slušajPartijuMulti() {
        listenerReg = db.collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists() || !isAdded()) return;
                    if (snapshot.getMetadata().isFromCache()) return;

                    String status = snapshot.getString("statusSkocko");
                    if (status == null) return;

                    Long b1L = snapshot.getLong("bodovi1Skocko");
                    Long b2L = snapshot.getLong("bodovi2Skocko");
                    int b1 = b1L != null ? b1L.intValue() : 0;
                    int b2 = b2L != null ? b2L.intValue() : 0;
                    binding.tvBodovi.setText("Bodovi: " + (jeIgrac1 ? b1 : b2));

                    syncBoardForWaitingPlayer(snapshot, status);

                    boolean statusChanged = !status.equals(lastSeenStatus);
                    if (!statusChanged) return;
                    lastSeenStatus = status;

                    switch (status) {
                        case RUNDA1_IGRAC1_IGRA:
                            if (jeIgrac1) {
                                // Igrac1 already started via inicijalizujRunda1KaoIgrac1()
                            } else {
                                setOverlay(true);
                                prikaziStatusMulti("🔴 Protivnik igra...");
                                startTajmerGledatelja(TRAJANJE_IGRAC);
                            }
                            break;

                        case RUNDA1_IGRAC1_BONUS:
                            if (!jeIgrac1) {
                                String tajna = snapshot.getString("tajnaKombinacijaR1Skocko");
                                tajnaKombinacija = tajna != null ? tajna.split(",") : new String[4];
                                setOverlay(false);
                                prikaziStatusMulti("🔵 Tvoj bonus (10s)!");
                                pokreniBonusRezim();
                            } else {
                                setOverlay(false);
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                                prikaziStatusMulti("🔵 Protivnik pokušava bonus...");
                                pocniBrojacBonusaGledatelja();
                            }
                            break;

                        case RUNDA1_PAUZA: {
                            if (!jeIgrac1)
                                rebuildBoardFromSync(snapshot.getString("tablaSyncR1"), snapshot.getString("feedbackSyncR1"));
                            String tajnaR1 = snapshot.getString("tajnaKombinacijaR1Skocko");
                            if (tajnaR1 != null) {
                                String[] tk = tajnaR1.split(",");
                                if (tk.length >= 4) {
                                    binding.btnSlotR8P1.setText(tk[0]); binding.btnSlotR8P2.setText(tk[1]);
                                    binding.btnSlotR8P3.setText(tk[2]); binding.btnSlotR8P4.setText(tk[3]);
                                }
                            }
                            String bonStr = snapshot.getString("bonusSyncR1");
                            String bonFb  = snapshot.getString("bonusFeedbackR1");
                            if (bonStr != null && !bonStr.isEmpty()) {
                                String[] bParts = bonStr.split(",");
                                for (int pi = 0; pi < bParts.length && pi < 4; pi++)
                                    bonusSlots[pi].setText("-".equals(bParts[pi]) ? "" : bParts[pi]);
                                if (bonFb != null && !bonFb.isEmpty()) {
                                    String[] fp = bonFb.split(",");
                                    if (fp.length >= 2) {
                                        try { setFeedbackKrugovi(6, Integer.parseInt(fp[0]), Integer.parseInt(fp[1])); }
                                        catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                            prikaziPauzu("Priprema 2. runde", "do početka 2. runde", () -> {
                                if (jeIgrac1)
                                    db.collection("partije").document(partijaId)
                                      .update("statusSkocko", RUNDA2_IGRAC2_IGRA);
                            });
                            break;
                        }

                        case RUNDA2_IGRAC2_IGRA:
                            resetBoardMulti();
                            aktivniRed = 0; aktivniSlot = 0;
                            bonusRezim = false;
                            odbrojavanjeAktivno = false;
                            if (!jeIgrac1) {
                                tajnaKombinacija = generisiKombinaciju();
                                String tajna = String.join(",", tajnaKombinacija);
                                db.collection("partije").document(partijaId)
                                  .update("tajnaKombinacijaR2Skocko", tajna);
                                setOverlay(false);
                                prikaziStatusMulti("🔵 Tvoj red!");
                                pocniIgruMulti();
                            } else {
                                setOverlay(true);
                                prikaziStatusMulti("🔵 Protivnik igra...");
                                startTajmerGledatelja(TRAJANJE_IGRAC);
                            }
                            break;

                        case RUNDA2_IGRAC2_BONUS:
                            bonusRezim = false;
                            bonusSlot = 0;
                            bonusPokusaj = new String[4];
                            for (Button b : bonusSlots) { b.setText(""); b.setEnabled(false); }
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            if (jeIgrac1) {
                                String tajna = snapshot.getString("tajnaKombinacijaR2Skocko");
                                tajnaKombinacija = tajna != null ? tajna.split(",") : new String[4];
                                setOverlay(false);
                                prikaziStatusMulti("🔴 Tvoj bonus (10s)!");
                                pokreniBonusRezim();
                            } else {
                                setOverlay(false);
                                setSimboliEnabled(false);
                                binding.btnPotvrdaPokusaja.setEnabled(false);
                                prikaziStatusMulti("🔴 Protivnik pokušava bonus...");
                                pocniBrojacBonusaGledatelja();
                            }
                            break;

                        case RUNDA2_PAUZA: {
                            if (jeIgrac1)
                                rebuildBoardFromSync(snapshot.getString("tablaSyncR2"), snapshot.getString("feedbackSyncR2"));
                            String tajnaR2 = snapshot.getString("tajnaKombinacijaR2Skocko");
                            if (tajnaR2 != null) {
                                String[] tk = tajnaR2.split(",");
                                if (tk.length >= 4) {
                                    binding.btnSlotR8P1.setText(tk[0]); binding.btnSlotR8P2.setText(tk[1]);
                                    binding.btnSlotR8P3.setText(tk[2]); binding.btnSlotR8P4.setText(tk[3]);
                                }
                            }
                            String bonStr2 = snapshot.getString("bonusSyncR2");
                            String bonFb2  = snapshot.getString("bonusFeedbackR2");
                            if (bonStr2 != null && !bonStr2.isEmpty()) {
                                String[] bParts = bonStr2.split(",");
                                for (int pi = 0; pi < bParts.length && pi < 4; pi++)
                                    bonusSlots[pi].setText("-".equals(bParts[pi]) ? "" : bParts[pi]);
                                if (bonFb2 != null && !bonFb2.isEmpty()) {
                                    String[] fp = bonFb2.split(",");
                                    if (fp.length >= 2) {
                                        try { setFeedbackKrugovi(6, Integer.parseInt(fp[0]), Integer.parseInt(fp[1])); }
                                        catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                            prikaziPauzu("Kraj Skočka", "do sledeće igre", () -> {
                                if (jeIgrac1)
                                    db.collection("partije").document(partijaId)
                                      .update("statusSkocko", ZAVRSENA);
                            });
                            break;
                        }

                        case ZAVRSENA:
                            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
                            setOverlay(false);
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
        db.collection("partije").document(partijaId).update("statusSkocko", sledeci);
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
        int[] fb = izracunajFeedbackBrojevi(pokusaj, tajnaKombinacija);
        feedbackBrojevi[aktivniRed][0] = fb[0];
        feedbackBrojevi[aktivniRed][1] = fb[1];
        setFeedbackKrugovi(aktivniRed, fb[0], fb[1]);
        {
            String tabKey = jeIgrac1 ? "tablaSyncR1" : "tablaSyncR2";
            String fbKey  = jeIgrac1 ? "feedbackSyncR1" : "feedbackSyncR2";
            db.collection("partije").document(partijaId)
              .update(tabKey, encodeTablaSync(aktivniRed + 1),
                      fbKey,  encodeFeedbackSync(aktivniRed + 1));
        }
        StringBuilder sbFb = new StringBuilder();
        for (int i = 0; i < fb[0]; i++) sbFb.append("●");
        for (int i = 0; i < fb[1]; i++) sbFb.append("○");
        feedbackovi[aktivniRed] = sbFb.toString();

        if (fb[0] == 4) {
            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
            int bodi = izracunajBodoveMulti();
            bodovi += bodi;
            String poljeB = jeIgrac1 ? "bodovi1Skocko" : "bodovi2Skocko";
            prikaziRjesenjeMulti();
            String sledeci = jeIgrac1 ? RUNDA1_PAUZA : RUNDA2_PAUZA;
            db.collection("partije").document(partijaId).update(poljeB, bodovi, "statusSkocko", sledeci);
            return;
        }

        aktivniRed++;
        aktivniSlot = 0;

        if (aktivniRed >= 6) {
            if (tajmer != null) { tajmer.cancel(); tajmer = null; }
            setSimboliEnabled(false);
            binding.btnPotvrdaPokusaja.setEnabled(false);
            prikaziRjesenjeMulti();
            String sledeci = jeIgrac1 ? RUNDA1_IGRAC1_BONUS : RUNDA2_IGRAC2_BONUS;
            db.collection("partije").document(partijaId).update("statusSkocko", sledeci);
        }
    }

    private void provjeriBonus() {
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
        int[] fb = izracunajFeedbackBrojevi(bonusPokusaj, tajnaKombinacija);
        setFeedbackKrugovi(6, fb[0], fb[1]);

        boolean pogodio = fb[0] == 4;
        String poljeB = jeIgrac1 ? "bodovi1Skocko" : "bodovi2Skocko";
        if (pogodio) bodovi += 10;

        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
        for (Button b : bonusSlots) b.setEnabled(false);

        // !jeIgrac1 bio je na bonusu R1 → sledeci R1_PAUZA
        // jeIgrac1 bio je na bonusu R2 → sledeci R2_PAUZA
        String bonusSyncKey = !jeIgrac1 ? "bonusSyncR1" : "bonusSyncR2";
        String bonusFbKey   = !jeIgrac1 ? "bonusFeedbackR1" : "bonusFeedbackR2";
        StringBuilder bonusBuilder = new StringBuilder();
        for (int pi = 0; pi < 4; pi++) {
            if (pi > 0) bonusBuilder.append(",");
            bonusBuilder.append(bonusPokusaj[pi] != null ? bonusPokusaj[pi] : "-");
        }
        String sledeci = !jeIgrac1 ? RUNDA1_PAUZA : RUNDA2_PAUZA;
        db.collection("partije").document(partijaId).update(
            poljeB, bodovi,
            bonusSyncKey, bonusBuilder.toString(),
            bonusFbKey, fb[0] + "," + fb[1],
            "statusSkocko", sledeci);
    }

    private void pokreniBonusRezim() {
        bonusRezim = true;
        bonusSlot = 0;
        bonusPokusaj = new String[4];
        for (Button b : bonusSlots) { b.setText(""); b.setEnabled(true); }
        setSimboliEnabled(true);
        binding.btnPotvrdaPokusaja.setEnabled(true);
        if (tajmer != null) tajmer.cancel();
        binding.tvTajmer.setText("Bonus: 10s");
        tajmer = new CountDownTimer(TRAJANJE_BONUS, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("Bonus: " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                tajmer = null;
                setSimboliEnabled(false);
                binding.btnPotvrdaPokusaja.setEnabled(false);
                for (Button b : bonusSlots) b.setEnabled(false);
                // Isteklo bez pogotka — idi na pauzu
                String sledeci = !jeIgrac1 ? RUNDA1_PAUZA : RUNDA2_PAUZA;
                db.collection("partije").document(partijaId).update("statusSkocko", sledeci);
            }
        }.start();
    }

    private void prikaziPauzu(String naslov, String poruka, Runnable callback) {
        if (odbrojavanjeAktivno) return;
        odbrojavanjeAktivno = true;
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
        setOverlay(false);
        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);

        final int[] sek = {5};
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
                    callback.run();
                }
            }
        };
        h.post(tick);
    }

    private int izracunajBodoveMulti() {
        if (aktivniRed <= 1) return 20;
        if (aktivniRed <= 3) return 15;
        return 10;
    }

    private void prikaziRjesenjeMulti() {
        binding.btnSlotR8P1.setText(tajnaKombinacija[0]);
        binding.btnSlotR8P2.setText(tajnaKombinacija[1]);
        binding.btnSlotR8P3.setText(tajnaKombinacija[2]);
        binding.btnSlotR8P4.setText(tajnaKombinacija[3]);
    }

    private void resetBoardMulti() {
        tabla = new String[6][4];
        feedbackovi = new String[6];
        feedbackBrojevi = new int[6][2];
        for (int r = 0; r < 6; r++) {
            for (int p = 0; p < 4; p++) slotButtons[r][p].setText("");
            setFeedbackKrugovi(r, 0, 0);
        }
        setFeedbackKrugovi(6, 0, 0);
        bonusRezim = false;
        bonusSlot = 0;
        bonusPokusaj = new String[4];
        for (Button b : bonusSlots) { b.setText(""); b.setEnabled(false); }
        binding.btnSlotR8P1.setText("");
        binding.btnSlotR8P2.setText("");
        binding.btnSlotR8P3.setText("");
        binding.btnSlotR8P4.setText("");
        if (tajmer != null) { tajmer.cancel(); tajmer = null; }
    }

    private void setOverlay(boolean prikazi) {
        binding.overlayProtivnik.setVisibility(prikazi ? View.VISIBLE : View.GONE);
    }

    private void prikaziStatusMulti(String tekst) {
        if (binding != null) binding.tvStatus.setText(tekst);
    }

    // ─────────────────────────────────────────────────────
    // ZAJEDNIČKI UTILITY
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
        for (int r = 0; r < 6; r++) {
            final int red = r;
            for (int p = 0; p < 4; p++) {
                final int pozicija = p;
                slotButtons[r][p].setOnClickListener(v -> klikNaSlotSolo(red, pozicija));
            }
        }
    }

    private int[] parseFeedback(String s) {
        if (s == null) return new int[]{0, 0};
        int tacno = 0, djel = 0;
        for (char c : s.toCharArray()) {
            if (c == '●') tacno++;
            else if (c == '○') djel++;
        }
        return new int[]{tacno, djel};
    }

    private String[] generisiKombinaciju() {
        String[] k = new String[4];
        Random rnd = new Random();
        for (int i = 0; i < 4; i++)
            k[i] = SkockoViewModel.SIMBOLI[rnd.nextInt(SkockoViewModel.SIMBOLI.length)];
        return k;
    }

    private void setSimboliEnabled(boolean enabled) {
        binding.btnSimbol1.setEnabled(enabled);
        binding.btnSimbol2.setEnabled(enabled);
        binding.btnSimbol3.setEnabled(enabled);
        binding.btnSimbol4.setEnabled(enabled);
        binding.btnSimbol5.setEnabled(enabled);
        binding.btnSimbol6.setEnabled(enabled);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String encodeTablaSync(int rowCount) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rowCount && r < 6; r++) {
            if (r > 0) sb.append("|");
            for (int p = 0; p < 4; p++) {
                if (p > 0) sb.append(",");
                sb.append(tabla[r][p] != null ? tabla[r][p] : "-");
            }
        }
        return sb.toString();
    }

    private String encodeFeedbackSync(int rowCount) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rowCount && r < 6; r++) {
            if (r > 0) sb.append("|");
            sb.append(feedbackBrojevi[r][0]).append(",").append(feedbackBrojevi[r][1]);
        }
        return sb.toString();
    }

    private void rebuildBoardFromSync(String tablaStr, String fbStr) {
        if (tablaStr == null || tablaStr.isEmpty()) return;
        String[] rows = tablaStr.split("\\|");
        String[] fbRows = fbStr != null && !fbStr.isEmpty() ? fbStr.split("\\|") : new String[0];
        for (int r = 0; r < rows.length && r < 6; r++) {
            String[] slots = rows[r].split(",");
            for (int p = 0; p < slots.length && p < 4; p++)
                slotButtons[r][p].setText("-".equals(slots[p]) ? "" : slots[p]);
            if (r < fbRows.length && !fbRows[r].isEmpty()) {
                String[] parts = fbRows[r].split(",");
                if (parts.length >= 2) {
                    try { setFeedbackKrugovi(r, Integer.parseInt(parts[0]), Integer.parseInt(parts[1])); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private void syncBoardForWaitingPlayer(DocumentSnapshot snapshot, String status) {
        switch (status) {
            case RUNDA1_IGRAC1_IGRA:
                if (!jeIgrac1)
                    rebuildBoardFromSync(snapshot.getString("tablaSyncR1"), snapshot.getString("feedbackSyncR1"));
                break;
            case RUNDA2_IGRAC2_IGRA:
                if (jeIgrac1)
                    rebuildBoardFromSync(snapshot.getString("tablaSyncR2"), snapshot.getString("feedbackSyncR2"));
                break;
        }
    }

    private void startTajmerGledatelja(long trajanje) {
        if (tajmer != null) tajmer.cancel();
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("⏱ " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                binding.tvTajmer.setText("⏱ 0s");
                tajmer = null;
            }
        }.start();
    }

    private void pocniBrojacBonusaGledatelja() {
        if (tajmer != null) tajmer.cancel();
        tajmer = new CountDownTimer(TRAJANJE_BONUS, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("Bonus: " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                binding.tvTajmer.setText("Bonus: 0s");
                tajmer = null;
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        if (listenerReg != null) listenerReg.remove();
        binding = null;
    }
}
