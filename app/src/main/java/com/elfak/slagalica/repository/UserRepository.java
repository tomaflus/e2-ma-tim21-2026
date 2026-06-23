package com.elfak.slagalica.repository;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnSuccessListener { void onSuccess(); }
    public interface OnErrorListener { void onError(String poruka); }
    public interface OnUserListener { void onUser(User user); }
    public interface OnUrlListener { void onUrl(String url); }

    public UserRepository() {}

    // PLAN B: Čuvanje slike kao Base64 string direktno u Firestore
    public void uploadSlikuBase64(android.content.Context context, Uri uri, OnUrlListener onUrl, OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
        
        new Thread(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                
                // Smanjujemo sliku da ne prepuni bazu (Firestore limit je 1MB po dokumentu)
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                
                String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
                
                // Upisujemo direktno u korisnički profil
                db.collection("users").document(auth.getCurrentUser().getUid())
                    .update("avatarUrl", base64Image)
                    .addOnSuccessListener(v -> mainHandler.post(() -> onUrl.onUrl(base64Image)))
                    .addOnFailureListener(e -> mainHandler.post(() -> onError.onError(e.getMessage())));
                    
            } catch (Exception e) {
                mainHandler.post(() -> onError.onError("Greška pri obradi slike: " + e.getMessage()));
            }
        }).start();
    }

    public void dohvatiKorisnika(OnUserListener onUser, OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(uid);
                        mainHandler.post(() -> onUser.onUser(user));
                    }
                })
                .addOnFailureListener(e -> mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    public void oduzmiToken(OnSuccessListener onSuccess, OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
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

                    db.collection("users").document(uid)
                            .update("tokeni", user.getTokeni() - 1)
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(onSuccess::onSuccess))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
                });
    }

    public void vratiToken() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .update("tokeni", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void dodajDnevneTokene(OnSuccessListener onSuccess, OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
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
                        League league = League.getByStars(user.getZvezde());
                        int tokeniZaDodati = 5 + league.getBonusTokens();
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
                });
    }

    public void azurirajAvatar(String url, OnSuccessListener onSuccess, OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .update("avatarUrl", url)
                .addOnSuccessListener(v -> mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e -> mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    public void azurirajZvezdeILigu(int noveZvezde, OnSuccessListener onSuccess) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        League current = League.getByStars(noveZvezde);
        db.collection("users").document(uid)
                .update("zvezde", noveZvezde, "liga", current.ordinal())
                .addOnSuccessListener(v -> mainHandler.post(onSuccess::onSuccess));
    }

    public void azurirajNakonPartije(boolean jePobjedio, int bodovi,
                                     boolean prijateljska,
                                     OnSuccessListener onSuccess,
                                     OnErrorListener onError) {
        if (prijateljska) {
            mainHandler.post(onSuccess::onSuccess);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user == null) return;

                    int zvezdaOdBodova = bodovi / 40;
                    int promjena;
                    if (jePobjedio) {
                        promjena = 10 + zvezdaOdBodova;
                    } else {
                        promjena = -10 + zvezdaOdBodova;
                    }

                    int noveZvezde = Math.max(0, user.getZvezde() + promjena);
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
}