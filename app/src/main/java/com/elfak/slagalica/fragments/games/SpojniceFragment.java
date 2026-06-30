package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentSpojniceBinding;
import com.elfak.slagalica.model.matching.MatchingPair;
import com.elfak.slagalica.repository.matching.MatchingRepository;
import com.elfak.slagalica.service.matching.MatchingService;
import com.elfak.slagalica.service.stats.StatsService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SpojniceFragment extends Fragment {

    // Firestore statusi
    private static final String SPJ_R1_IGRAC1 = "SPJ_R1_IGRAC1";
    private static final String SPJ_R1_BONUS   = "SPJ_R1_BONUS";
    private static final String SPJ_PAUZA      = "SPJ_PAUZA";
    private static final String SPJ_R2_IGRAC2  = "SPJ_R2_IGRAC2";
    private static final String SPJ_R2_BONUS   = "SPJ_R2_BONUS";
    private static final String SPJ_ZAVRSENA   = "SPJ_ZAVRSENA";

    private static final long TRAJANJE_AKTIVNOG = 30_000L;
    private static final long TRAJANJE_BONUSA   = 30_000L;

    private FragmentSpojniceBinding binding;
    private Button[] leftBtns;
    private Button[] rightBtns;
    private Button selectedLeft;

    // Lokalni/izazov režim
    private MatchingService service;
    private CountDownTimer localTimer;
    private boolean jeOrkestrirano;
    private boolean isGameOver;
    private int localAttemptCount; // attempts this local round

    // Multiplayer režim
    private String partijaId;
    private boolean jeIgrac1;
    private FirebaseFirestore db;
    private DocumentReference partijaRef;
    private ListenerRegistration spjListener;
    private MatchingRepository repo;
    private List<MatchingPair> pairsR1;
    private List<MatchingPair> pairsR2;
    private List<String> shuffledRightR1;
    private List<String> shuffledRightR2;
    private String lastSeenStatus;
    private int currentRound = 1;
    private CountDownTimer spjTimer;
    private boolean spjZavrsen;
    private int spjStartBodovi1;
    private int spjStartBodovi2;
    private final Set<String> localActiveAttempts = new HashSet<>();
    private final Set<String> localBonusAttempts  = new HashSet<>();
    private int bonusAvailable; // unmatched pairs available in bonus phase

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpojniceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        leftBtns  = new Button[]{binding.btnLeft1, binding.btnLeft2, binding.btnLeft3,
                                  binding.btnLeft4, binding.btnLeft5};
        rightBtns = new Button[]{binding.btnRight1, binding.btnRight2, binding.btnRight3,
                                  binding.btnRight4, binding.btnRight5};

        binding.btnBackToHome.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        Bundle args = getArguments();
        if (args != null) {
            partijaId       = args.getString("partijaId");
            jeIgrac1        = args.getBoolean("jeIgrac1", true);
            boolean jeIzazov = args.getBoolean("jeIzazov", false);
            jeOrkestrirano  = partijaId != null || jeIzazov;
            spjStartBodovi1 = args.getInt("startingScore1", 0);
            spjStartBodovi2 = args.getInt("startingScore2", 0);
        }

        if (partijaId != null) {
            initMultiplayer();
        } else {
            initLocal();
        }
    }

    // ─── LOKALNI REŽIM ──────────────────────────────────────────────────────────

    private void initLocal() {
        service = new MatchingService(new MatchingRepository(), new StatsService(requireContext()));

        binding.btnConfirm.setOnClickListener(v -> {
            if (service.getRound() < 2) {
                service.loadRound(2);
                renderLocal();
            } else {
                finishGameLocal();
            }
        });

        for (Button lb : leftBtns) {
            lb.setOnClickListener(v -> {
                if (selectedLeft != null) {
                    selectedLeft.setBackgroundColor(couleur(R.color.sivaNeaktivno));
                }
                selectedLeft = (Button) v;
                selectedLeft.setBackgroundColor(couleur(R.color.plavaSelektovano));
            });
        }

        for (Button rb : rightBtns) {
            rb.setOnClickListener(v -> {
                if (selectedLeft == null) return;
                Button left = selectedLeft;
                selectedLeft = null;
                // BUG FIX: disable left button after ANY attempt (correct or wrong)
                left.setEnabled(false);
                left.setBackgroundColor(couleur(R.color.sivaIskorisceno));
                localAttemptCount++;

                Button right = (Button) v;
                if (service.check(left.getText().toString(), right.getText().toString())) {
                    left.setBackgroundColor(couleur(R.color.zelenaTacno));
                    right.setBackgroundColor(couleur(R.color.zelenaTacno));
                    right.setEnabled(false);
                } else {
                    right.setBackgroundColor(couleur(R.color.crvenaNetacno));
                    right.postDelayed(() -> {
                        if (binding != null) right.setBackgroundColor(couleur(R.color.purple_500));
                    }, 500);
                }

                if (localAttemptCount >= 5) {
                    binding.btnConfirm.setVisibility(View.VISIBLE);
                }
            });
        }

        renderLocal();
    }

    private void renderLocal() {
        if (binding == null) return;
        selectedLeft = null;
        localAttemptCount = 0;
        binding.btnConfirm.setVisibility(View.GONE);

        for (int i = 0; i < 5; i++) {
            leftBtns[i].setText(service.getPairs().get(i).getLeftTerm());
            rightBtns[i].setText(service.getShuffledRight().get(i));
            leftBtns[i].setEnabled(true);
            rightBtns[i].setEnabled(true);
        }
        resetButtonColors();
        binding.tvRound.setText("Runda: " + service.getRound() + "/2");

        if (localTimer != null) localTimer.cancel();
        localTimer = new CountDownTimer(30_000, 1000) {
            public void onTick(long m) {
                if (binding != null) binding.tvTajmer.setText("⏱ " + m / 1000 + "s");
            }
            public void onFinish() {
                if (binding == null) return;
                if (service.getRound() < 2) { service.loadRound(2); renderLocal(); }
                else finishGameLocal();
            }
        }.start();
    }

    private void finishGameLocal() {
        if (isGameOver || binding == null) return;
        isGameOver = true;
        if (localTimer != null) localTimer.cancel();
        if (jeOrkestrirano) {
            Bundle b = new Bundle();
            b.putInt("bodovi", service.getScore());
            if (isAdded()) getParentFragmentManager().setFragmentResult("spojniceZavrsen", b);
            return;
        }
        service.save();
        binding.clGameContent.setVisibility(View.GONE);
        binding.clGameOver.setVisibility(View.VISIBLE);
        binding.tvFinalScore.setText("Osvoji ste: " + service.getScore() + " bodova");
    }

    // ─── MULTIPLAYER REŽIM ──────────────────────────────────────────────────────

    private void initMultiplayer() {
        db         = FirebaseFirestore.getInstance();
        partijaRef = db.collection("partije").document(partijaId);
        repo       = new MatchingRepository();

        binding.btnConfirm.setVisibility(View.GONE);

        // Listeners za dugmad — dele se i aktivnom i posmatraču (enabled kontroliše pristup)
        for (Button lb : leftBtns) lb.setOnClickListener(v -> onLeftClickMulti((Button) v));
        for (Button rb : rightBtns) rb.setOnClickListener(v -> onRightClickMulti((Button) v));

        prikaziPauzu(7, "Spojnice počinje za", () -> {
            if (jeIgrac1) inicijalizujSpjFirestore();
            pocniSlušatiSpj();
        });
    }

    private void inicijalizujSpjFirestore() {
        Random rnd = new Random();
        int idx1 = rnd.nextInt(11);
        int idx2;
        do { idx2 = rnd.nextInt(11); } while (idx2 == idx1);

        pairsR1 = repo.getSetByIndex(idx1);
        pairsR2 = repo.getSetByIndex(idx2);

        shuffledRightR1 = shuffleRight(pairsR1);
        shuffledRightR2 = shuffleRight(pairsR2);

        Map<String, Object> init = new HashMap<>();
        init.put("spjStatus",          SPJ_R1_IGRAC1);
        init.put("spjTimerEndMs",       System.currentTimeMillis() + TRAJANJE_AKTIVNOG);
        init.put("spjSetIdxR1",         (long) idx1);
        init.put("spjSetIdxR2",         (long) idx2);
        init.put("spjShuffledRightR1",  shuffledRightR1);
        init.put("spjShuffledRightR2",  shuffledRightR2);
        init.put("spjPovezanoR1",       new ArrayList<>());
        init.put("spjPovezanoR2",       new ArrayList<>());
        init.put("bodovi1Spj",          0L);
        init.put("bodovi2Spj",          0L);
        init.put("spjStartBodovi1",     (long) spjStartBodovi1);
        init.put("spjStartBodovi2",     (long) spjStartBodovi2);
        partijaRef.update(init);
    }

    private void pocniSlušatiSpj() {
        spjListener = partijaRef.addSnapshotListener((snap, e) -> {
            if (snap == null || binding == null || spjZavrsen) return;
            handleSnapshot(snap);
        });
    }

    private void handleSnapshot(DocumentSnapshot snap) {
        String status = snap.getString("spjStatus");
        if (status == null) return;

        // Igrac2 učitava parove pri prvom snapshotu
        if (pairsR1 == null && snap.getLong("spjSetIdxR1") != null) {
            pairsR1 = repo.getSetByIndex(snap.getLong("spjSetIdxR1").intValue());
        }
        if (pairsR2 == null && snap.getLong("spjSetIdxR2") != null) {
            pairsR2 = repo.getSetByIndex(snap.getLong("spjSetIdxR2").intValue());
        }
        if (shuffledRightR1 == null) shuffledRightR1 = getStringList(snap, "spjShuffledRightR1");
        if (shuffledRightR2 == null) shuffledRightR2 = getStringList(snap, "spjShuffledRightR2");

        if (status.equals(lastSeenStatus)) {
            // Isti status — samo ažuriramo boje (novi tačan spoj)
            azurirajBojeIzFirestore(snap);
            return;
        }
        lastSeenStatus = status;

        switch (status) {
            case SPJ_R1_IGRAC1:
                currentRound = 1;
                localActiveAttempts.clear();
                setupFaza(snap, jeIgrac1, false);
                break;
            case SPJ_R1_BONUS:
                currentRound = 1;
                localBonusAttempts.clear();
                bonusAvailable = 5 - getStringList(snap, "spjPovezanoR1").size();
                setupFaza(snap, !jeIgrac1, true);
                break;
            case SPJ_PAUZA:
                if (spjTimer != null) spjTimer.cancel();
                setOverlay(false);
                prikaziPauzu(5, "Runda 2 počinje za", () -> {
                    if (!jeIgrac1 && !spjZavrsen) inicijalizujR2();
                });
                break;
            case SPJ_R2_IGRAC2:
                currentRound = 2;
                localActiveAttempts.clear();
                setupFaza(snap, !jeIgrac1, false);
                break;
            case SPJ_R2_BONUS:
                currentRound = 2;
                localBonusAttempts.clear();
                bonusAvailable = 5 - getStringList(snap, "spjPovezanoR2").size();
                setupFaza(snap, jeIgrac1, true);
                break;
            case SPJ_ZAVRSENA:
                handleZavrsena();
                break;
        }
    }

    private void setupFaza(DocumentSnapshot snap, boolean jeAktivan, boolean jeBonusFaza) {
        if (spjTimer != null) spjTimer.cancel();
        if (binding == null) return;

        List<MatchingPair> pairs   = currentRound == 1 ? pairsR1 : pairsR2;
        List<String>       shuffled = currentRound == 1 ? shuffledRightR1 : shuffledRightR2;
        List<String>       pov     = getStringList(snap, currentRound == 1 ? "spjPovezanoR1" : "spjPovezanoR2");

        if (pairs == null || shuffled == null) return;

        // Postavi tekstove
        for (int i = 0; i < 5; i++) {
            leftBtns[i].setText(pairs.get(i).getLeftTerm());
            rightBtns[i].setText(shuffled.get(i));
        }
        resetButtonColors();
        selectedLeft = null;

        // Postavi stanja dugmadi
        for (int i = 0; i < 5; i++) {
            boolean matched = pov.contains(pairs.get(i).getLeftTerm());
            if (matched) {
                leftBtns[i].setEnabled(false);
                leftBtns[i].setBackgroundColor(couleur(R.color.zelenaTacno));
            } else {
                // U aktivnoj fazi, posmatrač ne može kliknuti; u bonus fazi sva nepolja su dostupna
                leftBtns[i].setEnabled(jeAktivan);
            }
        }
        for (int i = 0; i < 5; i++) {
            boolean rightMatched = isRightMatched(shuffled.get(i), pov, pairs);
            if (rightMatched) {
                rightBtns[i].setEnabled(false);
                rightBtns[i].setBackgroundColor(couleur(R.color.zelenaTacno));
            } else {
                rightBtns[i].setEnabled(jeAktivan);
            }
        }

        String runLabel = jeBonusFaza ? "Bonus — runda " + currentRound : "Runda: " + currentRound + "/2";
        binding.tvRound.setText(runLabel);
        binding.tvStatus.setVisibility(View.GONE);

        Long endMs    = snap.getLong("spjTimerEndMs");
        long remaining = endMs != null ? Math.max(0L, endMs - System.currentTimeMillis()) : 0L;

        if (!jeAktivan) {
            String msg = jeBonusFaza
                    ? "Ti si odigrao — protivnik pokušava preostale"
                    : "Protivnik igra runda " + currentRound;
            setOverlay(true, msg);
            startWatchTimer(remaining);
        } else {
            setOverlay(false);
            String expectedStatus = lastSeenStatus;
            startActiveTimer(remaining, expectedStatus);
        }
    }

    private void onLeftClickMulti(Button lb) {
        if (selectedLeft != null) {
            selectedLeft.setBackgroundColor(couleur(R.color.sivaNeaktivno));
        }
        selectedLeft = lb;
        lb.setBackgroundColor(couleur(R.color.plavaSelektovano));
    }

    private void onRightClickMulti(Button rb) {
        if (selectedLeft == null || spjZavrsen) return;
        Button left = selectedLeft;
        selectedLeft = null;

        // Odmah onemogući levo dugme (BUG FIX: važi i u multiplayer modu)
        left.setEnabled(false);
        left.setBackgroundColor(couleur(R.color.sivaIskorisceno));

        String leftTerm  = left.getText().toString();
        String rightTerm = rb.getText().toString();

        boolean isBonus = SPJ_R1_BONUS.equals(lastSeenStatus) || SPJ_R2_BONUS.equals(lastSeenStatus);

        // Ažuriraj lokalno praćenje pokušaja
        if (!isBonus) {
            localActiveAttempts.add(leftTerm);
        } else {
            localBonusAttempts.add(leftTerm);
        }

        // Provjeri tačnost
        List<MatchingPair> pairs = currentRound == 1 ? pairsR1 : pairsR2;
        boolean correct = false;
        if (pairs != null) {
            for (MatchingPair p : pairs) {
                if (p.getLeftTerm().equals(leftTerm) && p.getRightTerm().equals(rightTerm)) {
                    correct = true;
                    break;
                }
            }
        }

        if (correct) {
            left.setBackgroundColor(couleur(R.color.zelenaTacno));
            rb.setBackgroundColor(couleur(R.color.zelenaTacno));
            rb.setEnabled(false);

            String povField  = currentRound == 1 ? "spjPovezanoR1" : "spjPovezanoR2";
            String bField    = jeIgrac1 ? "bodovi1" : "bodovi2";
            String bSpjField = jeIgrac1 ? "bodovi1Spj" : "bodovi2Spj";

            Map<String, Object> upd = new HashMap<>();
            upd.put(povField,  FieldValue.arrayUnion(leftTerm));
            upd.put(bField,    FieldValue.increment(2));
            upd.put(bSpjField, FieldValue.increment(2));

            partijaRef.update(upd).addOnSuccessListener(__ -> checkAdvance(isBonus));
        } else {
            rb.setBackgroundColor(couleur(R.color.crvenaNetacno));
            rb.postDelayed(() -> {
                if (binding != null) rb.setBackgroundColor(couleur(R.color.purple_500));
            }, 500);
            checkAdvance(isBonus);
        }
    }

    private void checkAdvance(boolean isBonus) {
        if (!isBonus && localActiveAttempts.size() >= 5) {
            napredujAktivnaFaza();
        } else if (isBonus && localBonusAttempts.size() >= bonusAvailable) {
            napredujBonusFaza();
        }
    }

    private void napredujAktivnaFaza() {
        String expectedStatus = lastSeenStatus;
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(partijaRef);
            if (!expectedStatus.equals(snap.getString("spjStatus"))) return null;

            String povField  = currentRound == 1 ? "spjPovezanoR1" : "spjPovezanoR2";
            List<String> pov = getStringList(snap, povField);
            boolean allMatched = pov.size() >= 5;

            String nextStatus;
            long   nextTimer = 0L;
            if (currentRound == 1) {
                nextStatus = allMatched ? SPJ_PAUZA : SPJ_R1_BONUS;
            } else {
                nextStatus = allMatched ? SPJ_ZAVRSENA : SPJ_R2_BONUS;
            }
            if (SPJ_R1_BONUS.equals(nextStatus) || SPJ_R2_BONUS.equals(nextStatus)) {
                nextTimer = System.currentTimeMillis() + TRAJANJE_BONUSA;
            }

            Map<String, Object> upd = new HashMap<>();
            upd.put("spjStatus", nextStatus);
            if (nextTimer > 0) upd.put("spjTimerEndMs", nextTimer);
            transaction.update(partijaRef, upd);
            return null;
        });
    }

    private void napredujBonusFaza() {
        String expectedStatus = lastSeenStatus;
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(partijaRef);
            if (!expectedStatus.equals(snap.getString("spjStatus"))) return null;

            String nextStatus = currentRound == 1 ? SPJ_PAUZA : SPJ_ZAVRSENA;
            transaction.update(partijaRef, "spjStatus", nextStatus);
            return null;
        });
    }

    private void inicijalizujR2() {
        Map<String, Object> upd = new HashMap<>();
        upd.put("spjStatus",    SPJ_R2_IGRAC2);
        upd.put("spjTimerEndMs", System.currentTimeMillis() + TRAJANJE_AKTIVNOG);
        partijaRef.update(upd);
    }

    private void handleZavrsena() {
        if (spjZavrsen) return;
        spjZavrsen = true;
        if (spjTimer != null) spjTimer.cancel();
        setOverlay(false);
        // Prikaži odbrojavanje dok je Spojnice još vidljiv u pozadini
        prikaziPauzu(7, "Naredna igra: Asocijacije za", () -> {
            Bundle b = new Bundle();
            b.putInt("bodovi", 0); // bodovi1/2 su ažurirani u Firestore po spoju
            if (isAdded()) getParentFragmentManager().setFragmentResult("spojniceZavrsen", b);
        });
    }

    private void azurirajBojeIzFirestore(DocumentSnapshot snap) {
        if (pairsR1 == null || pairsR2 == null || binding == null) return;
        List<MatchingPair> pairs   = currentRound == 1 ? pairsR1 : pairsR2;
        List<String>       shuffled = currentRound == 1 ? shuffledRightR1 : shuffledRightR2;
        List<String>       pov     = getStringList(snap, currentRound == 1 ? "spjPovezanoR1" : "spjPovezanoR2");
        markMatched(pov, pairs, shuffled);
    }

    private void markMatched(List<String> pov, List<MatchingPair> pairs, List<String> shuffled) {
        if (pov == null || pairs == null || shuffled == null) return;
        int green = couleur(R.color.zelenaTacno);
        for (String lt : pov) {
            for (int i = 0; i < pairs.size(); i++) {
                if (pairs.get(i).getLeftTerm().equals(lt)) {
                    leftBtns[i].setBackgroundColor(green);
                    leftBtns[i].setEnabled(false);
                    int ri = shuffled.indexOf(pairs.get(i).getRightTerm());
                    if (ri >= 0) {
                        rightBtns[ri].setBackgroundColor(green);
                        rightBtns[ri].setEnabled(false);
                    }
                    break;
                }
            }
        }
    }

    private void startActiveTimer(long durationMs, String expectedStatus) {
        if (spjTimer != null) spjTimer.cancel();
        spjTimer = new CountDownTimer(durationMs, 1000) {
            public void onTick(long m) {
                if (binding != null) binding.tvTajmer.setText("⏱ " + m / 1000 + "s");
            }
            public void onFinish() {
                if (binding == null) return;
                boolean isBonus = SPJ_R1_BONUS.equals(expectedStatus) || SPJ_R2_BONUS.equals(expectedStatus);
                if (isBonus) napredujBonusFaza();
                else napredujAktivnaFaza();
            }
        }.start();
    }

    private void startWatchTimer(long remaining) {
        if (spjTimer != null) spjTimer.cancel();
        if (remaining <= 0) {
            if (binding != null) binding.tvTajmer.setText("⏱ 0s");
            return;
        }
        spjTimer = new CountDownTimer(remaining, 1000) {
            public void onTick(long m) {
                if (binding != null) binding.tvTajmer.setText("⏱ " + m / 1000 + "s");
            }
            public void onFinish() {
                if (binding != null) binding.tvTajmer.setText("⏱ 0s");
            }
        }.start();
    }

    // ─── POMOĆNE METODE ─────────────────────────────────────────────────────────

    private void prikaziPauzu(int sekunde, String prefiks, Runnable callback) {
        if (binding == null) return;
        binding.tvStatus.setVisibility(View.VISIBLE);
        new CountDownTimer(sekunde * 1000L + 100, 1000) {
            public void onTick(long m) {
                if (binding != null) binding.tvStatus.setText(prefiks + " " + (m / 1000) + "s");
            }
            public void onFinish() {
                if (binding != null) binding.tvStatus.setVisibility(View.GONE);
                new Handler(Looper.getMainLooper()).post(callback::run);
            }
        }.start();
    }

    private void setOverlay(boolean show) {
        setOverlay(show, "");
    }

    private void setOverlay(boolean show, String msg) {
        if (binding == null) return;
        binding.overlayProtivnik.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) binding.tvOverlayMsg.setText(msg);
    }

    private void resetButtonColors() {
        int leftC  = couleur(R.color.sivaNeaktivno);
        int rightC = couleur(R.color.purple_500);
        for (Button lb : leftBtns)  lb.setBackgroundColor(leftC);
        for (Button rb : rightBtns) rb.setBackgroundColor(rightC);
    }

    private boolean isRightMatched(String rightTerm, List<String> pov, List<MatchingPair> pairs) {
        if (pov == null || pairs == null) return false;
        for (String lt : pov) {
            for (MatchingPair p : pairs) {
                if (p.getLeftTerm().equals(lt) && p.getRightTerm().equals(rightTerm)) return true;
            }
        }
        return false;
    }

    private List<String> shuffleRight(List<MatchingPair> pairs) {
        List<String> right = new ArrayList<>();
        for (MatchingPair p : pairs) right.add(p.getRightTerm());
        Collections.shuffle(right);
        return right;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(DocumentSnapshot snap, String field) {
        Object val = snap.get(field);
        if (val instanceof List) return (List<String>) val;
        return new ArrayList<>();
    }

    private int couleur(int colorRes) {
        return getResources().getColor(colorRes, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (localTimer != null) localTimer.cancel();
        if (spjTimer != null)   spjTimer.cancel();
        if (spjListener != null) spjListener.remove();
        binding = null;
    }
}
