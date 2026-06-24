package com.elfak.slagalica.service;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.DnevneMisije;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

public class DnevneMisijeService {

    // Čuvanje: users/{uid}/dnevneMisije/current (podkolekcija "dnevneMisije", dokument "current")
    // Ukupne zvezde se ažuriraju, ciklusne zvezde NE — misije su van rang-liste bodovanja.

    public enum TipMisije { POBEDA, PORUKA_CET, PRIJATELJSKA, TURNIR_POBEDA }

    public interface OnMisijeLoaded { void onLoaded(DnevneMisije misije); }
    public interface OnOznaciResult {
        void onResult(int zvezdeDodati, int tokeniDodati, boolean sveZavrseno);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DocumentReference getMisijeRef(String uid) {
        return db.collection("users").document(uid)
                .collection("dnevneMisije").document("current");
    }

    public void ucitajMisije(String uid, OnMisijeLoaded onLoaded) {
        getMisijeRef(uid).get()
                .addOnSuccessListener(snapshot -> {
                    DnevneMisije misije;
                    if (snapshot.exists()) {
                        misije = snapshot.toObject(DnevneMisije.class);
                        if (misije == null) misije = noviDan();
                    } else {
                        misije = noviDan();
                    }
                    if (!CiklusUtil.trenutniDanId().equals(misije.getDanId())) {
                        DnevneMisije resetovane = noviDan();
                        getMisijeRef(uid).set(resetovane);
                        final DnevneMisije r = resetovane;
                        mainHandler.post(() -> onLoaded.onLoaded(r));
                    } else {
                        final DnevneMisije m = misije;
                        mainHandler.post(() -> onLoaded.onLoaded(m));
                    }
                })
                .addOnFailureListener(e -> mainHandler.post(() -> onLoaded.onLoaded(noviDan())));
    }

    public void oznaciMisiju(String uid, TipMisije tip, OnOznaciResult onResult) {
        DocumentReference misijeRef = getMisijeRef(uid);
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<int[]>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(misijeRef);

            DnevneMisije misije;
            if (snapshot.exists()) {
                misije = snapshot.toObject(DnevneMisije.class);
                if (misije == null) misije = noviDan();
            } else {
                misije = noviDan();
            }

            if (!CiklusUtil.trenutniDanId().equals(misije.getDanId())) {
                misije = noviDan();
            }

            if (jeVecUradjeno(misije, tip)) {
                return new int[]{0, 0, 0};
            }

            oznaci(misije, tip);

            int zvezdeDodati = 3;
            int tokeniDodati = 0;
            int bonusFlag = 0;

            if (sveJeZavrseno(misije) && !misije.isBonusDodeljen()) {
                misije.setBonusDodeljen(true);
                zvezdeDodati += 3;
                tokeniDodati = 2;
                bonusFlag = 1;
            }

            transaction.set(misijeRef, misije);
            transaction.update(userRef, "zvezde", FieldValue.increment(zvezdeDodati));
            if (tokeniDodati > 0) {
                transaction.update(userRef, "tokeni", FieldValue.increment(tokeniDodati));
            }

            return new int[]{zvezdeDodati, tokeniDodati, bonusFlag};
        }).addOnSuccessListener(result -> {
            if (result != null) {
                mainHandler.post(() -> onResult.onResult(result[0], result[1], result[2] == 1));
            }
        }).addOnFailureListener(e -> mainHandler.post(() -> onResult.onResult(0, 0, false)));
    }

    // TODO: Pozovi oznaciMisiju(uid, TipMisije.TURNIR_POBEDA, callback) iz TurnirFragment-a kad se implementira.

    private DnevneMisije noviDan() {
        DnevneMisije m = new DnevneMisije();
        m.setDanId(CiklusUtil.trenutniDanId());
        return m;
    }

    private boolean jeVecUradjeno(DnevneMisije m, TipMisije tip) {
        switch (tip) {
            case POBEDA:          return m.isPobedaUradjeno();
            case PORUKA_CET:      return m.isPorukaCetUradjeno();
            case PRIJATELJSKA:    return m.isPrijateljskaUradjeno();
            case TURNIR_POBEDA:   return m.isTurnirPobedaUradjeno();
            default:              return true;
        }
    }

    private void oznaci(DnevneMisije m, TipMisije tip) {
        switch (tip) {
            case POBEDA:        m.setPobedaUradjeno(true);       break;
            case PORUKA_CET:    m.setPorukaCetUradjeno(true);    break;
            case PRIJATELJSKA:  m.setPrijateljskaUradjeno(true); break;
            case TURNIR_POBEDA: m.setTurnirPobedaUradjeno(true); break;
        }
    }

    private boolean sveJeZavrseno(DnevneMisije m) {
        return m.isPobedaUradjeno() && m.isPorukaCetUradjeno()
                && m.isPrijateljskaUradjeno() && m.isTurnirPobedaUradjeno();
    }
}
