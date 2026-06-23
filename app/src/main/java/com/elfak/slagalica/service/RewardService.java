package com.elfak.slagalica.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.helpers.NotifikacijaHelper;
import com.elfak.slagalica.model.NagradaInfo;
import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.RangRepository;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RewardService {

    public interface OnNagradaListener {
        void onNagrada(NagradaInfo info);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final RangRepository rangRepository = new RangRepository();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void proveriINagradi(Context context, String uid, OnNagradaListener onNagrada) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) {
                        mainHandler.post(() -> onNagrada.onNagrada(null));
                        return;
                    }
                    user.setId(uid);

                    String noviNedeljaId = CiklusUtil.trenutniNedeljaId();
                    String noviMesecId = CiklusUtil.trenutniMesecId();
                    String stariNedeljaId = user.getNedeljaCiklusId();
                    String stariMesecId = user.getMesecCiklusId();

                    boolean nedeljaPromenjena = !stariNedeljaId.isEmpty()
                            && !stariNedeljaId.equals(noviNedeljaId)
                            && user.isRangiranNedelja()
                            && !stariNedeljaId.equals(user.getLastNagradaNedeljaId());

                    boolean mesecPromenjen = !stariMesecId.isEmpty()
                            && !stariMesecId.equals(noviMesecId)
                            && user.isRangiranMesec()
                            && !stariMesecId.equals(user.getLastNagradaMesecId());

                    if (!nedeljaPromenjena && !mesecPromenjen) {
                        mainHandler.post(() -> onNagrada.onNagrada(null));
                        return;
                    }

                    int[] pending = {(nedeljaPromenjena ? 1 : 0) + (mesecPromenjen ? 1 : 0)};
                    int[] tokeniNedelja = {0};
                    int[] pozicijaNedelja = {-1};
                    int[] tokeniMesec = {0};
                    int[] pozicijaMesec = {-1};

                    Runnable onBothDone = () -> {
                        if (--pending[0] > 0) return;

                        int ukupno = tokeniNedelja[0] + tokeniMesec[0];
                        Map<String, Object> updates = new HashMap<>();
                        if (nedeljaPromenjena) updates.put("lastNagradaNedeljaId", stariNedeljaId);
                        if (mesecPromenjen) updates.put("lastNagradaMesecId", stariMesecId);
                        if (ukupno > 0) updates.put("tokeni", FieldValue.increment(ukupno));

                        db.collection("users").document(uid).update(updates)
                                .addOnSuccessListener(v -> {
                                    if (ukupno > 0) {
                                        NagradaInfo info = new NagradaInfo(
                                                tokeniNedelja[0], pozicijaNedelja[0],
                                                tokeniMesec[0], pozicijaMesec[0]);
                                        NotifikacijaHelper.prikaziNotifikacijuNagradu(context, ukupno);
                                        sacuvajNotifikacijuNagrade(uid, info);
                                        mainHandler.post(() -> onNagrada.onNagrada(info));
                                    } else {
                                        mainHandler.post(() -> onNagrada.onNagrada(null));
                                    }
                                })
                                .addOnFailureListener(e ->
                                        mainHandler.post(() -> onNagrada.onNagrada(null)));
                    };

                    if (nedeljaPromenjena) {
                        rangRepository.dohvatiNedeljnuRang(stariNedeljaId, lista -> {
                            int poz = nadjiPoziciju(lista, uid);
                            pozicijaNedelja[0] = poz;
                            tokeniNedelja[0] = tokeniNedelja(poz);
                            onBothDone.run();
                        });
                    }

                    if (mesecPromenjen) {
                        rangRepository.dohvatiMesecnuRang(stariMesecId, lista -> {
                            int poz = nadjiPoziciju(lista, uid);
                            pozicijaMesec[0] = poz;
                            tokeniMesec[0] = tokeniMesec(poz);
                            onBothDone.run();
                        });
                    }
                })
                .addOnFailureListener(e -> mainHandler.post(() -> onNagrada.onNagrada(null)));
    }

    private int nadjiPoziciju(List<User> lista, String uid) {
        for (int i = 0; i < lista.size(); i++) {
            if (uid.equals(lista.get(i).getId())) return i + 1;
        }
        return -1;
    }

    private int tokeniNedelja(int poz) {
        if (poz == 1) return 5;
        if (poz == 2) return 3;
        if (poz == 3) return 2;
        if (poz >= 4 && poz <= 10) return 1;
        return 0;
    }

    private int tokeniMesec(int poz) {
        if (poz == 1) return 10;
        if (poz == 2) return 6;
        if (poz == 3) return 4;
        if (poz >= 4 && poz <= 10) return 2;
        return 0;
    }

    private void sacuvajNotifikacijuNagrade(String uid, NagradaInfo info) {
        String datumVrijeme = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        StringBuilder sadrzaj = new StringBuilder();
        if (info.pozicijaNedelja > 0) {
            sadrzaj.append(info.pozicijaNedelja).append(". nedeljne rang liste — +")
                    .append(info.tokeniNedelja).append(" tokena. ");
        }
        if (info.pozicijaMesec > 0) {
            sadrzaj.append(info.pozicijaMesec).append(". mesečne rang liste — +")
                    .append(info.tokeniMesec).append(" tokena.");
        }
        Notifikacija n = new Notifikacija("nagrade", "Nagrade za rang listu",
                sadrzaj.toString().trim(), datumVrijeme, false);
        db.collection("users").document(uid)
                .collection("notifikacije").add(n);
    }
}
