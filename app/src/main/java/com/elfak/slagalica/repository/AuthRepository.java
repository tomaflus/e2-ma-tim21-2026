package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Registracija
    public void registracija(String email, String lozinka, String korisnickoIme,
                             String region, OnSuccessListener onSuccess, OnErrorListener onError) {
        auth.createUserWithEmailAndPassword(email, lozinka)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    firebaseUser.sendEmailVerification();

                    User user = new User(
                            firebaseUser.getUid(),
                            email,
                            korisnickoIme,
                            region
                    );

                    db.collection("users")
                            .document(firebaseUser.getUid())
                            .set(user)
                            .addOnSuccessListener(unused ->
                                    mainHandler.post(onSuccess::onSuccess))
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onError.onError(e.getMessage())));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Logovanje
    public void logovanje(String emailIliKorisnickoIme, String lozinka,
                          OnSuccessListener onSuccess, OnErrorListener onError) {
        if (emailIliKorisnickoIme.contains("@")) {
            loginSaEmailom(emailIliKorisnickoIme, lozinka, onSuccess, onError);
        } else {
            db.collection("users")
                    .whereEqualTo("korisnickoIme", emailIliKorisnickoIme)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            mainHandler.post(() ->
                                    onError.onError("Korisnicko ime nije pronadjeno!"));
                            return;
                        }
                        String email = query.getDocuments().get(0).getString("email");
                        loginSaEmailom(email, lozinka, onSuccess, onError);
                    })
                    .addOnFailureListener(e ->
                            mainHandler.post(() -> onError.onError(e.getMessage())));
        }
    }

    private void loginSaEmailom(String email, String lozinka,
                                OnSuccessListener onSuccess, OnErrorListener onError) {
        auth.signInWithEmailAndPassword(email, lozinka)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    if (!firebaseUser.isEmailVerified()) {
                        mainHandler.post(() ->
                                onError.onError("Email nije verifikovan! Provjerite inbox."));
                        auth.signOut();
                        return;
                    }

                    mainHandler.post(onSuccess::onSuccess);
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Odjava
    public void odjava() {
        auth.signOut();
    }

    // Da li je korisnik ulogovan
    public boolean jeUlogovan() {
        return auth.getCurrentUser() != null
                && auth.getCurrentUser().isEmailVerified();
    }

    // Trenutni korisnik
    public FirebaseUser trenutniKorisnik() {
        return auth.getCurrentUser();
    }

    // Reset lozinke
    public void resetLozinke(String email, OnSuccessListener onSuccess, OnErrorListener onError) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Interfejsi za callbacks
    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }
}