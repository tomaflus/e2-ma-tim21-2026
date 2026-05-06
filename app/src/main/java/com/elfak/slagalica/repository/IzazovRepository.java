package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Izazov;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IzazovRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration listenerRegistration;

    public interface OnIzazoviListener {
        void onIzazovi(List<Izazov> izazovi);
    }

    public interface OnIzazovListener {
        void onIzazov(Izazov izazov);
    }

    public interface OnSuccessListener {
        void onSuccess(String izazovId);
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }

    // Kreiraj novi izazov — bez oduzimanja, kreator placa kad pokrene
    public void kreirajIzazov(String kreatorIme, String region,
                              int zvezdeUlog, int tokeniUlog,
                              OnSuccessListener onSuccess,
                              OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();
        Izazov izazov = new Izazov(uid, kreatorIme, region, zvezdeUlog, tokeniUlog);

        db.collection("izazovi")
                .add(izazov)
                .addOnSuccessListener(ref ->
                        mainHandler.post(() -> onSuccess.onSuccess(ref.getId())))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Prihvati izazov — oduzmi zvezde i tokene od igraca koji prihvata
    public void prihvatiIzazov(String izazovId, String igracIme,
                               OnSuccessListener onSuccess,
                               OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("izazovi").document(izazovId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Izazov izazov = snapshot.toObject(Izazov.class);
                    if (izazov == null) return;

                    // Provjeri da li je vec u izazovu
                    if (izazov.getIgraciIds().contains(uid)) {
                        mainHandler.post(() ->
                                onError.onError("Vec ste u ovom izazovu!"));
                        return;
                    }

                    // Provjeri max 4 igraca
                    if (izazov.getIgraciIds().size() >= 4) {
                        mainHandler.post(() ->
                                onError.onError("Izazov je pun!"));
                        return;
                    }

                    // Oduzmi zvezde i tokene od igraca koji prihvata
                    db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                Long zvezde = userSnapshot.getLong("zvezde");
                                Long tokeni = userSnapshot.getLong("tokeni");

                                int novaZvezde = (zvezde != null ?
                                        zvezde.intValue() : 0) - izazov.getZvezdeUlog();
                                int noviTokeni = (tokeni != null ?
                                        tokeni.intValue() : 0) - izazov.getTokeniUlog();

                                db.collection("users").document(uid)
                                        .update("zvezde", novaZvezde, "tokeni", noviTokeni)
                                        .addOnSuccessListener(unused -> {
                                            // Dodaj igraca u izazov
                                            izazov.getIgraciIds().add(uid);
                                            izazov.getIgraciImena().add(igracIme);

                                            db.collection("izazovi").document(izazovId)
                                                    .update(
                                                            "igraciIds", izazov.getIgraciIds(),
                                                            "igraciImena", izazov.getIgraciImena()
                                                    )
                                                    .addOnSuccessListener(u ->
                                                            mainHandler.post(() ->
                                                                    onSuccess.onSuccess(izazovId)))
                                                    .addOnFailureListener(e ->
                                                            mainHandler.post(() ->
                                                                    onError.onError(e.getMessage())));
                                        });
                            });
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Pokreni izazov — oduzmi zvezde i tokene od kreatora i postavi status U_TOKU
    public void pokreniIzazov(String izazovId, OnSuccessListener onSuccess,
                              OnErrorListener onError) {
        db.collection("izazovi").document(izazovId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Izazov izazov = snapshot.toObject(Izazov.class);
                    if (izazov == null) return;

                    String kreatorId = izazov.getKreatorId();

                    // Oduzmi zvezde i tokene od kreatora
                    db.collection("users").document(kreatorId)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                Long zvezde = userSnapshot.getLong("zvezde");
                                Long tokeni = userSnapshot.getLong("tokeni");

                                int novaZvezde = (zvezde != null ?
                                        zvezde.intValue() : 0) - izazov.getZvezdeUlog();
                                int noviTokeni = (tokeni != null ?
                                        tokeni.intValue() : 0) - izazov.getTokeniUlog();

                                db.collection("users").document(kreatorId)
                                        .update("zvezde", novaZvezde, "tokeni", noviTokeni)
                                        .addOnSuccessListener(unused ->
                                                // Postavi status na U_TOKU
                                                db.collection("izazovi").document(izazovId)
                                                        .update("status", "U_TOKU")
                                                        .addOnSuccessListener(u ->
                                                                mainHandler.post(() ->
                                                                        onSuccess.onSuccess(izazovId)))
                                                        .addOnFailureListener(e ->
                                                                mainHandler.post(() ->
                                                                        onError.onError(e.getMessage()))));
                            });
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Dohvati aktivne izazove za region
    public void slušajIzazove(String region, OnIzazoviListener onIzazovi,
                              OnErrorListener onError) {
        listenerRegistration = db.collection("izazovi")
                .whereEqualTo("region", region)
                .whereEqualTo("status", "AKTIVAN")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        mainHandler.post(() -> onError.onError(e.getMessage()));
                        return;
                    }
                    if (snapshot != null) {
                        List<Izazov> izazovi = new ArrayList<>();
                        for (var doc : snapshot.getDocuments()) {
                            Izazov izazov = doc.toObject(Izazov.class);
                            if (izazov != null) {
                                izazov.setId(doc.getId());
                                izazovi.add(izazov);
                            }
                        }
                        mainHandler.post(() -> onIzazovi.onIzazovi(izazovi));
                    }
                });
    }

    // Slušaj konkretan izazov
    public void slušajIzazov(String izazovId, OnIzazovListener onIzazov,
                             OnErrorListener onError) {
        db.collection("izazovi").document(izazovId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        mainHandler.post(() -> onError.onError(e.getMessage()));
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Izazov izazov = snapshot.toObject(Izazov.class);
                        if (izazov != null) {
                            izazov.setId(snapshot.getId());
                            mainHandler.post(() -> onIzazov.onIzazov(izazov));
                        }
                    }
                });
    }

    // Sacuvaj rezultat igraca
    public void sacuvajRezultat(String izazovId, int bodovi,
                                OnSuccessListener onSuccess,
                                OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("izazovi").document(izazovId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Izazov izazov = snapshot.toObject(Izazov.class);
                    if (izazov == null) return;

                    Map<String, Integer> rezultati = izazov.getRezultati();
                    if (rezultati == null) rezultati = new HashMap<>();
                    rezultati.put(uid, bodovi);

                    // Provjeri da li su svi igraci zavrsili
                    boolean sviZavrsili = rezultati.size() >= izazov.getIgraciIds().size();
                    String noviStatus = sviZavrsili ? "ZAVRSEN" : "U_TOKU";

                    Map<String, Integer> finalRezultati = rezultati;
                    db.collection("izazovi").document(izazovId)
                            .update(
                                    "rezultati", finalRezultati,
                                    "status", noviStatus
                            )
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(() -> onSuccess.onSuccess(izazovId)))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Rasporedi nagrade
    public void rasporediNagrade(String izazovId,
                                 OnSuccessListener onSuccess,
                                 OnErrorListener onError) {
        db.collection("izazovi").document(izazovId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Izazov izazov = snapshot.toObject(Izazov.class);
                    if (izazov == null) return;

                    Map<String, Integer> rezultati = izazov.getRezultati();
                    if (rezultati == null || rezultati.isEmpty()) return;

                    int ukupnoZvezde = izazov.getZvezdeUlog() * izazov.getIgraciIds().size();
                    int ukupnoTokeni = izazov.getTokeniUlog() * izazov.getIgraciIds().size();

                    // Sortiraj po bodovima
                    List<Map.Entry<String, Integer>> sortirani =
                            new ArrayList<>(rezultati.entrySet());
                    sortirani.sort((a, b) -> b.getValue() - a.getValue());

                    // Pobjednik dobija 75%
                    String pobjednikId = sortirani.get(0).getKey();
                    int pobjednikZvezde = (int)(ukupnoZvezde * 0.75);
                    int pobjednikTokeni = (int)(ukupnoTokeni * 0.75);

                    // Drugi dobija nazad ulozeno
                    String drugiId = sortirani.size() > 1 ?
                            sortirani.get(1).getKey() : null;

                    // Azuriraj pobjednika
                    db.collection("users").document(pobjednikId)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                Long zvezde = userSnapshot.getLong("zvezde");
                                Long tokeni = userSnapshot.getLong("tokeni");
                                int noveZvezde = (zvezde != null ?
                                        zvezde.intValue() : 0) + pobjednikZvezde;
                                int noviTokeni = (tokeni != null ?
                                        tokeni.intValue() : 0) + pobjednikTokeni;

                                db.collection("users").document(pobjednikId)
                                        .update("zvezde", noveZvezde, "tokeni", noviTokeni);
                            });

                    // Azuriraj drugog
                    if (drugiId != null) {
                        String finalDrugiId = drugiId;
                        db.collection("users").document(drugiId)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    Long zvezde = userSnapshot.getLong("zvezde");
                                    Long tokeni = userSnapshot.getLong("tokeni");
                                    int noveZvezde = (zvezde != null ?
                                            zvezde.intValue() : 0) + izazov.getZvezdeUlog();
                                    int noviTokeni = (tokeni != null ?
                                            tokeni.intValue() : 0) + izazov.getTokeniUlog();

                                    db.collection("users").document(finalDrugiId)
                                            .update("zvezde", noveZvezde, "tokeni", noviTokeni);
                                });
                    }

                    mainHandler.post(() -> onSuccess.onSuccess(izazovId));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    public void ukloniListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}