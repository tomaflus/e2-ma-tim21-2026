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
import com.elfak.slagalica.databinding.FragmentKoZnaZnaBinding;
import com.elfak.slagalica.model.quiz.Question;
import com.elfak.slagalica.repository.quiz.QuizRepository;
import com.elfak.slagalica.service.quiz.QuizService;
import com.elfak.slagalica.service.stats.StatsService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KoZnaZnaFragment extends Fragment {

    private static final long TRAJANJE_PITANJA = 5000;
    private static final long TRAJANJE_PAUZE   = 3000;

    private FragmentKoZnaZnaBinding binding;

    // LOCAL / IZAZOV
    private QuizService service;
    private CountDownTimer localTimer;
    private boolean isGameOver = false;
    private boolean jeOrkestrirano = false;

    // MULTIPLAYER
    private String partijaId;
    private boolean jeIgrac1;
    private FirebaseFirestore db;
    private DocumentReference partijaRef;
    private ListenerRegistration kzzListener;
    private List<Question> pool;
    private List<Question> kzzPitanjaList;
    private int kzzTrenutnoQLocal = -1;
    private CountDownTimer kzzQTimer;
    private boolean kzzOdgovorio = false;
    private boolean kzzZavrsen = false;
    private boolean odbrojavanjeAktivno = false;
    private int kzzMojOdgovor = -1;
    private int kzzStartBodovi1 = 0;
    private int kzzStartBodovi2 = 0;
    private DocumentSnapshot lastKzzSnap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKoZnaZnaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean jeIzazov = false;
        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
            jeIzazov = getArguments().getBoolean("jeIzazov", false);
            jeOrkestrirano = (partijaId != null) || jeIzazov;
            kzzStartBodovi1 = getArguments().getInt("startingScore1", 0);
            kzzStartBodovi2 = getArguments().getInt("startingScore2", 0);
        }

        if (partijaId != null && !jeIzazov) {
            db = FirebaseFirestore.getInstance();
            partijaRef = db.collection("partije").document(partijaId);
            pool = new QuizRepository().getMockQuestions();
            initMultiplayer();
        } else {
            initLocal();
        }
    }

    // ── LOCAL / IZAZOV ───────────────────────────────────────────────────────

    private void initLocal() {
        service = new QuizService(new QuizRepository(), new StatsService(requireContext()));

        View.OnClickListener listener = v -> {
            int i = (v.getId() == R.id.btnAnswer1) ? 0
                  : (v.getId() == R.id.btnAnswer2) ? 1
                  : (v.getId() == R.id.btnAnswer3) ? 2 : 3;
            processAnswerLocal(i, (Button) v);
        };
        binding.btnAnswer1.setOnClickListener(listener);
        binding.btnAnswer2.setOnClickListener(listener);
        binding.btnAnswer3.setOnClickListener(listener);
        binding.btnAnswer4.setOnClickListener(listener);
        binding.btnBackToHome.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        renderLocal();
    }

    private void processAnswerLocal(int i, Button b) {
        if (localTimer != null) localTimer.cancel();
        setDugmadEnabled(false);
        int points = service.answer(i);
        boolean ok = points > 0;
        b.setBackgroundColor(getResources().getColor(
                ok ? R.color.zelenaTacno : R.color.crvenaNetacno, null));
        b.postDelayed(this::renderLocal, 1000);
    }

    private void renderLocal() {
        if (binding == null) return;
        if (service.isOver()) { finishGameLocal(); return; }
        Question q = service.getCurrent();
        binding.tvQuestion.setText(q.getText());
        binding.btnAnswer1.setText(q.getAnswers()[0]);
        binding.btnAnswer2.setText(q.getAnswers()[1]);
        binding.btnAnswer3.setText(q.getAnswers()[2]);
        binding.btnAnswer4.setText(q.getAnswers()[3]);
        binding.tvQuestionNumber.setText(
                getString(R.string.label_question_count, service.getIndex() + 1));
        resetBojeDugmadi();
        setDugmadEnabled(true);
        startLocalTimer();
    }

    private void startLocalTimer() {
        if (localTimer != null) localTimer.cancel();
        localTimer = new CountDownTimer(TRAJANJE_PITANJA, 1000) {
            public void onTick(long ms) {
                if (binding != null)
                    binding.tvTajmer.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            public void onFinish() {
                if (binding == null) return;
                binding.tvTajmer.setText("⏱ 0s");
                setDugmadEnabled(false);
                service.skip();
                binding.btnAnswer1.postDelayed(() -> renderLocal(), 1000);
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
            if (isAdded()) getParentFragmentManager()
                    .setFragmentResult("koZnaZnaZavrsen", b);
        } else {
            service.save();
            binding.clGameContent.setVisibility(View.GONE);
            binding.clGameOver.setVisibility(View.VISIBLE);
            binding.tvFinalScore.setText(getString(R.string.score_won, service.getScore()));
        }
    }

    // ── MULTIPLAYER ──────────────────────────────────────────────────────────

    private void initMultiplayer() {
        setDugmadEnabled(false);
        binding.tvTajmer.setText("");

        View.OnClickListener listener = v -> {
            if (kzzOdgovorio || kzzTrenutnoQLocal < 0) return;
            int i = (v.getId() == R.id.btnAnswer1) ? 0
                  : (v.getId() == R.id.btnAnswer2) ? 1
                  : (v.getId() == R.id.btnAnswer3) ? 2 : 3;
            onIgracOdgovorioMulti(kzzTrenutnoQLocal, i, (Button) v);
        };
        binding.btnAnswer1.setOnClickListener(listener);
        binding.btnAnswer2.setOnClickListener(listener);
        binding.btnAnswer3.setOnClickListener(listener);
        binding.btnAnswer4.setOnClickListener(listener);

        prikaziPauzu(5, "Ko Zna Zna počinje za", () -> {
            if (jeIgrac1) inicijalizujKzzFirestore();
            pocniSlušatiKzz();
        });
    }

    private void inicijalizujKzzFirestore() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) available.add(i);
        Collections.shuffle(available);

        List<Long> pitanjaIdx = new ArrayList<>();
        for (int i = 0; i < 5; i++) pitanjaIdx.add((long) available.get(i));

        List<Long> odgInit = Arrays.asList(-1L, -1L, -1L, -1L, -1L);
        List<Long> tsInit  = Arrays.asList(0L, 0L, 0L, 0L, 0L);

        Map<String, Object> init = new HashMap<>();
        init.put("kzzStatus",     "KZZ_PITANJE");
        init.put("kzzTrenutnoQ",  0L);
        init.put("kzzPitanja",    pitanjaIdx);
        init.put("kzzOdg1",       new ArrayList<>(odgInit));
        init.put("kzzOdg2",       new ArrayList<>(odgInit));
        init.put("kzzTs1",        new ArrayList<>(tsInit));
        init.put("kzzTs2",        new ArrayList<>(tsInit));
        init.put("kzzQTimerEndMs",    System.currentTimeMillis() + TRAJANJE_PITANJA);
        init.put("bodovi1Kzz",        0L);
        init.put("bodovi2Kzz",        0L);
        init.put("kzzStartBodovi1",   (long) kzzStartBodovi1);
        init.put("kzzStartBodovi2",   (long) kzzStartBodovi2);
        partijaRef.update(init);
    }

    @SuppressWarnings("unchecked")
    private void pocniSlušatiKzz() {
        kzzListener = partijaRef.addSnapshotListener((snap, e) -> {
            if (snap == null || !isAdded() || binding == null) return;
            lastKzzSnap = snap;

            String status = snap.getString("kzzStatus");
            if (status == null) return;

            if ("KZZ_ZAVRSENA".equals(status) && !kzzZavrsen) {
                kzzZavrsen = true;
                handleKzzZavrsena(snap);
                return;
            }

            if (!"KZZ_PITANJE".equals(status)) return;

            Long qLong    = snap.getLong("kzzTrenutnoQ");
            Long endMsLong = snap.getLong("kzzQTimerEndMs");
            if (qLong == null || endMsLong == null) return;

            int q = qLong.intValue();

            if (kzzPitanjaList == null) {
                List<Long> idxList = (List<Long>) snap.get("kzzPitanja");
                if (idxList == null || idxList.size() < 5) return;
                kzzPitanjaList = new ArrayList<>();
                for (Long idx : idxList) kzzPitanjaList.add(pool.get(idx.intValue()));
            }

            if (q != kzzTrenutnoQLocal) {
                kzzTrenutnoQLocal = q;
                kzzOdgovorio = false;
                kzzMojOdgovor = -1;
                prikaziNovoPitanje(q, endMsLong);
            }
        });
    }

    private void prikaziNovoPitanje(int q, long endMs) {
        long now      = System.currentTimeMillis();
        long pauseEnd = endMs - TRAJANJE_PITANJA;

        if (q > 0 && now < pauseEnd) {
            setDugmadEnabled(false);
            int[] sek = {(int)((pauseEnd - now) / 1000) + 1};
            Handler h = new Handler(Looper.getMainLooper());
            Runnable tick = new Runnable() {
                @Override public void run() {
                    if (!isAdded() || binding == null) return;
                    if (sek[0] > 0) {
                        binding.tvStatus.setVisibility(View.VISIBLE);
                        binding.tvStatus.setText("Sledeće pitanje za " + sek[0] + "s");
                        binding.tvTajmer.setText("");
                        sek[0]--;
                        h.postDelayed(this, 1000);
                    } else {
                        startKzzPitanje(q, endMs);
                    }
                }
            };
            h.post(tick);
        } else {
            startKzzPitanje(q, endMs);
        }
    }

    private void startKzzPitanje(int q, long endMs) {
        if (!isAdded() || binding == null) return;

        Question question = kzzPitanjaList.get(q);
        binding.tvQuestionNumber.setText("Pitanje " + (q + 1) + "/5");
        binding.tvQuestion.setText(question.getText());
        binding.btnAnswer1.setText(question.getAnswers()[0]);
        binding.btnAnswer2.setText(question.getAnswers()[1]);
        binding.btnAnswer3.setText(question.getAnswers()[2]);
        binding.btnAnswer4.setText(question.getAnswers()[3]);
        resetBojeDugmadi();
        setDugmadEnabled(true);
        binding.tvStatus.setVisibility(View.GONE);
        binding.tvFeedback.setVisibility(View.GONE);

        long remaining = endMs - System.currentTimeMillis();
        if (remaining <= 0) remaining = 500;
        startKzzQTimer(q, remaining);
    }

    private void startKzzQTimer(int q, long trajanje) {
        if (kzzQTimer != null) kzzQTimer.cancel();
        kzzQTimer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                if (binding != null) binding.tvTajmer.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (binding != null) {
                    binding.tvTajmer.setText("⏱ 0s");
                    setDugmadEnabled(false);
                }
                revealCorrectAnswer(q);
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> napraviTransakcijuNapretka(q), 800);
            }
        }.start();
    }

    @SuppressWarnings("unchecked")
    private void onIgracOdgovorioMulti(int q, int odgovorIdx, Button dugme) {
        kzzOdgovorio = true;
        kzzMojOdgovor = odgovorIdx;
        setDugmadEnabled(false);

        long ts = System.currentTimeMillis();
        String odgField = jeIgrac1 ? "kzzOdg1" : "kzzOdg2";
        String tsField  = jeIgrac1 ? "kzzTs1"  : "kzzTs2";

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(partijaRef);
            List<Long> odg = (List<Long>) snap.get(odgField);
            List<Long> tsL = (List<Long>) snap.get(tsField);
            if (odg == null || odg.size() <= q || odg.get(q) >= 0) return null;

            List<Long> odgNew = new ArrayList<>(odg);
            List<Long> tsNew  = (tsL != null) ? new ArrayList<>(tsL)
                                              : new ArrayList<>(Arrays.asList(0L,0L,0L,0L,0L));
            odgNew.set(q, (long) odgovorIdx);
            if (q < tsNew.size()) tsNew.set(q, ts);

            Map<String, Object> upd = new HashMap<>();
            upd.put(odgField, odgNew);
            upd.put(tsField, tsNew);
            transaction.update(partijaRef, upd);
            return null;
        });

        // Local visual feedback
        boolean tacno = (odgovorIdx == kzzPitanjaList.get(q).getCorrectAnswerIndex());
        dugme.setBackgroundColor(getResources().getColor(
                tacno ? R.color.zelenaTacno : R.color.crvenaNetacno, null));
    }

    @SuppressWarnings("unchecked")
    private void napraviTransakcijuNapretka(int q) {
        if (!isAdded()) return;
        final List<Question> localPool = pool;

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(partijaRef);
            Long current = snap.getLong("kzzTrenutnoQ");
            if (current == null || current.intValue() != q) return null;

            List<Long> odg1 = (List<Long>) snap.get("kzzOdg1");
            List<Long> odg2 = (List<Long>) snap.get("kzzOdg2");
            List<Long> ts1  = (List<Long>) snap.get("kzzTs1");
            List<Long> ts2  = (List<Long>) snap.get("kzzTs2");
            List<Long> pit  = (List<Long>) snap.get("kzzPitanja");
            if (pit == null || pit.size() <= q) return null;

            int correct = localPool.get(pit.get(q).intValue()).getCorrectAnswerIndex();
            long a1 = (odg1 != null && q < odg1.size()) ? odg1.get(q) : -1L;
            long a2 = (odg2 != null && q < odg2.size()) ? odg2.get(q) : -1L;
            long t1 = (ts1  != null && q < ts1.size())  ? ts1.get(q)  : 0L;
            long t2 = (ts2  != null && q < ts2.size())  ? ts2.get(q)  : 0L;

            boolean c1 = (a1 == correct), c2 = (a2 == correct);
            int d1 = 0, d2 = 0;
            if      (c1 && c2) { if (t1 <= t2) d1 = 10; else d2 = 10; }
            else if (c1)       { d1 = 10; }
            else if (c2)       { d2 = 10; }
            if (!c1 && a1 >= 0) d1 = -5;
            if (!c2 && a2 >= 0) d2 = -5;

            long b1 = valOrZero(snap.getLong("bodovi1Kzz")) + d1;
            long b2 = valOrZero(snap.getLong("bodovi2Kzz")) + d2;
            long start1 = valOrZero(snap.getLong("kzzStartBodovi1"));
            long start2 = valOrZero(snap.getLong("kzzStartBodovi2"));

            Map<String, Object> upd = new HashMap<>();
            upd.put("bodovi1Kzz", b1);
            upd.put("bodovi2Kzz", b2);
            upd.put("bodovi1", start1 + b1);
            upd.put("bodovi2", start2 + b2);

            long nextQ = q + 1;
            if (nextQ >= 5) {
                upd.put("kzzStatus", "KZZ_ZAVRSENA");
            } else {
                upd.put("kzzTrenutnoQ", nextQ);
                upd.put("kzzQTimerEndMs",
                        System.currentTimeMillis() + TRAJANJE_PAUZE + TRAJANJE_PITANJA);
            }

            transaction.update(partijaRef, upd);
            return null;
        });
    }

    private void handleKzzZavrsena(DocumentSnapshot snap) {
        if (kzzQTimer != null) kzzQTimer.cancel();
        // bodovi1/bodovi2 su već ažurirani per-pitanje direktno u Firestore,
        // šaljemo 0 da IgraFragment ne doda drugi put
        Bundle result = new Bundle();
        result.putInt("bodovi", 0);
        if (isAdded()) getParentFragmentManager()
                .setFragmentResult("koZnaZnaZavrsen", result);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private void prikaziPauzu(int sekunde, String poruka, Runnable callback) {
        if (odbrojavanjeAktivno) return;
        odbrojavanjeAktivno = true;
        binding.tvStatus.setVisibility(View.VISIBLE);
        int[] sek = {sekunde};
        Handler h = new Handler(Looper.getMainLooper());
        Runnable tick = new Runnable() {
            @Override public void run() {
                if (!isAdded() || binding == null) return;
                if (sek[0] > 0) {
                    binding.tvStatus.setText(poruka + " " + sek[0] + "s");
                    sek[0]--;
                    h.postDelayed(this, 1000);
                } else {
                    odbrojavanjeAktivno = false;
                    binding.tvStatus.setVisibility(View.GONE);
                    callback.run();
                }
            }
        };
        h.post(tick);
    }

    private void setDugmadEnabled(boolean enabled) {
        if (binding == null) return;
        binding.btnAnswer1.setEnabled(enabled);
        binding.btnAnswer2.setEnabled(enabled);
        binding.btnAnswer3.setEnabled(enabled);
        binding.btnAnswer4.setEnabled(enabled);
    }

    private void resetBojeDugmadi() {
        int c = getResources().getColor(R.color.purple_500, null);
        binding.btnAnswer1.setBackgroundColor(c);
        binding.btnAnswer2.setBackgroundColor(c);
        binding.btnAnswer3.setBackgroundColor(c);
        binding.btnAnswer4.setBackgroundColor(c);
    }

    @SuppressWarnings("unchecked")
    private void revealCorrectAnswer(int q) {
        if (!isAdded() || binding == null || kzzPitanjaList == null) return;
        int correct = kzzPitanjaList.get(q).getCorrectAnswerIndex();
        Button[] btns = {binding.btnAnswer1, binding.btnAnswer2,
                         binding.btnAnswer3, binding.btnAnswer4};
        if (kzzMojOdgovor != correct) {
            btns[correct].setBackgroundColor(0xFF9E9E9E);
        }

        if (lastKzzSnap != null) {
            List<Long> odg1 = (List<Long>) lastKzzSnap.get("kzzOdg1");
            List<Long> odg2 = (List<Long>) lastKzzSnap.get("kzzOdg2");
            List<Long> ts1  = (List<Long>) lastKzzSnap.get("kzzTs1");
            List<Long> ts2  = (List<Long>) lastKzzSnap.get("kzzTs2");

            long a1 = (odg1 != null && q < odg1.size()) ? odg1.get(q) : -1L;
            long a2 = (odg2 != null && q < odg2.size()) ? odg2.get(q) : -1L;
            long t1 = (ts1  != null && q < ts1.size())  ? ts1.get(q)  : 0L;
            long t2 = (ts2  != null && q < ts2.size())  ? ts2.get(q)  : 0L;

            long myA   = jeIgrac1 ? a1 : a2;
            long oppA  = jeIgrac1 ? a2 : a1;
            long myTs  = jeIgrac1 ? t1 : t2;
            long oppTs = jeIgrac1 ? t2 : t1;

            boolean iCorrect   = (myA == correct);
            boolean oppCorrect = (oppA == correct);

            String feedback;
            if (iCorrect && oppCorrect) {
                feedback = (myTs <= oppTs) ? "Tačan odgovor! +10 poena" : "Protivnik je bio brži! 0 poena";
            } else if (iCorrect) {
                feedback = "Tačan odgovor! +10 poena";
            } else if (oppCorrect) {
                feedback = (myA >= 0) ? "Pogrešan odgovor! −5 poena" : "Protivnik odgovorio tačno! 0 poena";
            } else {
                feedback = (myA >= 0) ? "Pogrešan odgovor! −5 poena" : "Isteklo vreme! 0 poena";
            }

            binding.tvFeedback.setText(feedback);
            binding.tvFeedback.setVisibility(View.VISIBLE);
        }
    }

    private long valOrZero(Long v) { return v != null ? v : 0L; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (localTimer != null) localTimer.cancel();
        if (kzzQTimer != null)  kzzQTimer.cancel();
        if (kzzListener != null) kzzListener.remove();
        binding = null;
    }
}
