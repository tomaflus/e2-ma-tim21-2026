package com.elfak.slagalica.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.helpers.NotifikacijaHelper;
import com.elfak.slagalica.model.NagradaInfo;
import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.model.RangLista;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.RangRepository;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    // Nagrade se dijele isključivo kroz obradiKrajCiklusa() (simulacijski dugme / kraj realnog ciklusa).
    // Ova metoda samo prosljeđuje na provjeru neprocitanih notifikacija o nagradama.
    public void proveriINagradi(Context context, String uid, OnNagradaListener onNagrada) {
        mainHandler.post(() -> onNagrada.onNagrada(null));
    }

    // Automatska provjera na kraju kalendarskog perioda (poziva WorkManager).
    // Procesira samo ako se kalendarski dio ciklusId-a razlikuje od tekućeg perioda.
    public void obradiPrirodniKrajCiklusa(Context context, Runnable onComplete) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        obradiPrirodniTip(context, currentUid, "nedeljna", () ->
                obradiPrirodniTip(context, currentUid, "mesecna", () ->
                        mainHandler.post(onComplete)));
    }

    private void obradiPrirodniTip(Context context, String currentUid, String tip, Runnable onDone) {
        db.collection("rangListe")
                .whereEqualTo("tip", tip)
                .whereEqualTo("aktivna", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) { onDone.run(); return; }
                    String aktivniCiklusId = snaps.getDocuments().get(0).getString("ciklusId");
                    if (aktivniCiklusId == null) { onDone.run(); return; }

                    String aktivniKal = CiklusUtil.kalendarskiDio(aktivniCiklusId);
                    String trenutniKal = tip.equals("nedeljna")
                            ? CiklusUtil.trenutniNedeljaId() : CiklusUtil.trenutniMesecId();

                    if (aktivniKal.equals(trenutniKal)) {
                        // Ciklus još nije završen — ništa ne radi
                        onDone.run();
                        return;
                    }

                    // Kalendarski period se promijenio — obradi kraj ciklusa.
                    // Novi ciklusId počinje od 1 u novom periodu (ne inkrementira stari).
                    String noviCiklusId = trenutniKal + "-1";
                    obradiTipSaNoviCiklusId(context, currentUid, tip,
                            snaps.getDocuments().get(0), aktivniCiklusId, noviCiklusId, onDone);
                })
                .addOnFailureListener(e -> onDone.run());
    }

    // Obrađuje kraj nedeljnog i mesečnog ciklusa za SVE korisnike koji su rangirani.
    // Čita aktivne rang liste iz "rangListe" kolekcije, zatvara ih i kreja nove.
    public void obradiKrajCiklusa(Context context, Runnable onComplete) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Oba ciklusa procesiramo u sekvencu: nedeljna → mesecna → done
        obradiTip(context, currentUid, "nedeljna", () ->
                obradiTip(context, currentUid, "mesecna", () ->
                        mainHandler.post(onComplete)));
    }

    private void obradiTip(Context context, String currentUid, String tip, Runnable onDone) {
        db.collection("rangListe")
                .whereEqualTo("tip", tip)
                .whereEqualTo("aktivna", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        String bazniId = tip.equals("nedeljna")
                                ? CiklusUtil.trenutniNedeljaId() : CiklusUtil.trenutniMesecId();
                        kreirajNovuRangListu(tip, bazniId);
                        onDone.run();
                        return;
                    }
                    com.google.firebase.firestore.DocumentSnapshot rlDoc = snaps.getDocuments().get(0);
                    String ciklusId = rlDoc.getString("ciklusId");
                    if (ciklusId == null) { onDone.run(); return; }
                    // Simulacija: inkrementiraj ciklusId unutar istog perioda
                    String noviCiklusId = CiklusUtil.sledeceCiklusId(ciklusId);
                    izvrsiObradu(context, currentUid, tip, rlDoc, ciklusId, noviCiklusId, onDone);
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void obradiTipSaNoviCiklusId(Context context, String currentUid, String tip,
                                          com.google.firebase.firestore.DocumentSnapshot rlDoc,
                                          String ciklusId, String noviCiklusId, Runnable onDone) {
        izvrsiObradu(context, currentUid, tip, rlDoc, ciklusId, noviCiklusId, onDone);
    }

    private void izvrsiObradu(Context context, String currentUid, String tip,
                               com.google.firebase.firestore.DocumentSnapshot rlDoc,
                               String ciklusId, String noviCiklusId, Runnable onDone) {
        String ciklusPolje = tip.equals("nedeljna") ? "nedeljaCiklusId" : "mesecCiklusId";
        String zvijezdePolje = tip.equals("nedeljna") ? "nedeljneZvezde" : "mesecneZvezde";
        String rangiranPolje = tip.equals("nedeljna") ? "rangiranNedelja" : "rangiranMesec";
        String lastNagradaPolje = tip.equals("nedeljna") ? "lastNagradaNedeljaId" : "lastNagradaMesecId";

        db.collection("users")
                .whereEqualTo(ciklusPolje, ciklusId)
                .get()
                .addOnSuccessListener(userSnaps -> {
                    List<User> rangirani = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : userSnaps) {
                        User u = doc.toObject(User.class);
                        u.setId(doc.getId());
                        boolean rangiran = tip.equals("nedeljna")
                                ? u.isRangiranNedelja() : u.isRangiranMesec();
                        boolean vecNagrađen = ciklusId.equals(
                                tip.equals("nedeljna") ? u.getLastNagradaNedeljaId() : u.getLastNagradaMesecId());
                        if (rangiran && !vecNagrađen) rangirani.add(u);
                    }
                    Collections.sort(rangirani, (a, b) -> {
                        int va = tip.equals("nedeljna") ? a.getNedeljneZvezde() : a.getMesecneZvezde();
                        int vb = tip.equals("nedeljna") ? b.getNedeljneZvezde() : b.getMesecneZvezde();
                        return vb - va;
                    });
                    if (rangirani.size() > 10) rangirani = rangirani.subList(0, 10);

                    for (QueryDocumentSnapshot doc : userSnaps) {
                        Map<String, Object> r = new HashMap<>();
                        r.put(ciklusPolje, "");
                        r.put(zvijezdePolje, 0);
                        r.put(rangiranPolje, false);
                        r.put(lastNagradaPolje, "");
                        db.collection("users").document(doc.getId()).update(r);
                    }

                    for (int i = 0; i < rangirani.size(); i++) {
                        User u = rangirani.get(i);
                        int poz = i + 1;
                        int tok = tip.equals("nedeljna") ? tokeniNedelja(poz) : tokeniMesec(poz);
                        if (tok > 0)
                            db.collection("users").document(u.getId())
                                    .update("tokeni", FieldValue.increment(tok));
                        db.collection("users").document(u.getId()).update(lastNagradaPolje, ciklusId);

                        NagradaInfo info = tip.equals("nedeljna")
                                ? new NagradaInfo(tok, poz, 0, -1)
                                : new NagradaInfo(0, -1, tok, poz);
                        sacuvajNotifikacijuNagrade(u.getId(), info);

                        if (u.getId().equals(currentUid) && tok > 0)
                            NotifikacijaHelper.prikaziNotifikacijuNagradu(context, tok);
                    }

                    rlDoc.getReference().update("aktivna", false);
                    String opseg = tip.equals("nedeljna") ? CiklusUtil.opsegNedelje() : CiklusUtil.opsegMeseca();
                    db.collection("rangListe").add(new RangLista(tip, noviCiklusId, opseg, true));

                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void kreirajNovuRangListu(String tip, String bazniId) {
        // Koristi se samo kad nema aktivne rang liste (inicijalizacija)
        String ciklusId = CiklusUtil.sledeceCiklusId(bazniId);
        String opseg = tip.equals("nedeljna")
                ? CiklusUtil.opsegNedelje() : CiklusUtil.opsegMeseca();
        db.collection("rangListe").add(new RangLista(tip, ciklusId, opseg, true));
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
        Notifikacija n = new Notifikacija(
                "🏆", "nagrade", "Nagrade za rang listu",
                sadrzaj.toString().trim(), datumVrijeme, false,
                info.tokeniNedelja, info.pozicijaNedelja,
                info.tokeniMesec, info.pozicijaMesec);
        db.collection("users").document(uid)
                .collection("notifikacije").add(n);
    }
}
