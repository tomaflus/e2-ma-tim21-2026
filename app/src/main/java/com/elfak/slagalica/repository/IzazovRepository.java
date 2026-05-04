package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Izazov;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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

    // Kreiraj novi izazov
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

    // Prihvati izazov
    public void prihvatiIzazov(String izazovId, String igraIme,
                               OnSuccessListener onSuccess,
                               OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("izazovi").document(izazovId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Izazov izazov = snapshot.toObject(Izazov.class);
                    if (izazov == null) return;

                    // Provjeri da li je već u izazovu
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

                    // Dodaj igraca
                    izazov.getIgraciIds().add(uid);
                    izazov.getIgraciImena().add(igraIme);

                    db.collection("izazovi").document(izazovId)
                            .update(
                                    "igraciIds", izazov.getIgraciIds(),
                                    "igraciImena", izazov.getIgraciImena()
                            )
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(() -> onSuccess.onSuccess(izazovId)))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
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
                        List<Izazov> izazovi = new java.util.ArrayList<>();
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

    // Sačuvaj rezultat igrača
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

                    // Provjeri da li su svi igraci završili
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
                            new java.util.ArrayList<>(rezultati.entrySet());
                    sortirani.sort((a, b) -> b.getValue() - a.getValue());

                    // Pobjednik dobija 75%
                    String pobjednikId = sortirani.get(0).getKey();
                    int pobjednikZvezde = (int)(ukupnoZvezde * 0.75);
                    int pobjednikTokeni = (int)(ukupnoTokeni * 0.75);

                    // Drugi dobija nazad uloženo
                    String drugiId = sortirani.size() > 1 ?
                            sortirani.get(1).getKey() : null;

                    // Ažuriraj pobjednika
                    db.collection("users").document(pobjednikId)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                Long zvezde = userSnapshot.getLong("zvezde");
                                Long tokeni = userSnapshot.getLong("tokeni");
                                int noveZvezde = (zvezde != null ? zvezde.intValue() : 0)
                                        + pobjednikZvezde;
                                int noviTokeni = (tokeni != null ? tokeni.intValue() : 0)
                                        + pobjednikTokeni;

                                db.collection("users").document(pobjednikId)
                                        .update("zvezde", noveZvezde, "tokeni", noviTokeni);
                            });

                    // Ažuriraj drugog
                    if (drugiId != null) {
                        String finalDrugiId = drugiId;
                        db.collection("users").document(drugiId)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    Long zvezde = userSnapshot.getLong("zvezde");
                                    Long tokeni = userSnapshot.getLong("tokeni");
                                    int noveZvezde = (zvezde != null ? zvezde.intValue() : 0)
                                            + izazov.getZvezdeUlog();
                                    int noviTokeni = (tokeni != null ? tokeni.intValue() : 0)
                                            + izazov.getTokeniUlog();

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