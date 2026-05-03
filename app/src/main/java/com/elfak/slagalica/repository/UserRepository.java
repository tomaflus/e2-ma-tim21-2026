package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }

    public interface OnUserListener {
        void onUser(User user);
    }

    // Dohvati trenutnog korisnika
    public void dohvatiKorisnika(OnUserListener onUser, OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(uid);
                        mainHandler.post(() -> onUser.onUser(user));
                    }
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Provjeri tokene i oduzmi 1
    public void oduzmiToken(OnSuccessListener onSuccess, OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) return;

                    if (user.getTokeni() <= 0) {
                        mainHandler.post(() -> onError.onError("Nemate tokena! Sacekajte sutra."));
                        return;
                    }

                    // Oduzmi token
                    db.collection("users").document(uid)
                            .update("tokeni", user.getTokeni() - 1)
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(onSuccess::onSuccess))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Ažuriraj zvezde i tokene nakon partije
    public void azurirajNakonPartije(boolean jePobjedio, int bodovi,
                                     boolean prijateljska,
                                     OnSuccessListener onSuccess,
                                     OnErrorListener onError) {
        if (prijateljska) {
            // Prijateljska partija — bez zvezdi
            mainHandler.post(onSuccess::onSuccess);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) return;

                    // Bodovi za zvezde: po 1 zvezda za svakih 40 bodova
                    int zvezdaOdBodova = bodovi / 40;

                    int promjena;
                    if (jePobjedio) {
                        promjena = 10 + zvezdaOdBodova;
                    } else {
                        promjena = -10 + zvezdaOdBodova;
                    }

                    // Zvezdama ne može ići ispod 0
                    int noveZvezde = Math.max(0, user.getZvezde() + promjena);

                    // 50 zvezda = 1 token
                    int noviTokeni = user.getTokeni();
                    int staroLiga = user.getZvezde() / 50;
                    int novoLiga = noveZvezde / 50;
                    if (novoLiga > staroLiga) {
                        noviTokeni += (novoLiga - staroLiga);
                    }

                    db.collection("users").document(uid)
                            .update(
                                    "zvezde", noveZvezde,
                                    "tokeni", noviTokeni
                            )
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(onSuccess::onSuccess))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Dodaj dnevne tokene (poziva se pri svakom loginu)
    public void dodajDnevneTokene(OnSuccessListener onSuccess, OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) return;

                    long trenutnoVrijeme = System.currentTimeMillis();
                    long zadnjiLogin = user.getZadnjiLoginDatum();
                    long jedanDan = 24 * 60 * 60 * 1000L;

                    if (trenutnoVrijeme - zadnjiLogin >= jedanDan) {
                        // Prošlo je više od 24h — dodaj tokene
                        int ligaBonus = user.getLiga();
                        int tokeniZaDodati = 5 + ligaBonus;
                        int noviTokeni = user.getTokeni() + tokeniZaDodati;

                        db.collection("users").document(uid)
                                .update(
                                        "tokeni", noviTokeni,
                                        "zadnjiLoginDatum", trenutnoVrijeme
                                )
                                .addOnSuccessListener(unused ->
                                        mainHandler.post(onSuccess::onSuccess))
                                .addOnFailureListener(e ->
                                        mainHandler.post(() ->
                                                onError.onError(e.getMessage())));
                    } else {
                        mainHandler.post(onSuccess::onSuccess);
                    }
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }
}