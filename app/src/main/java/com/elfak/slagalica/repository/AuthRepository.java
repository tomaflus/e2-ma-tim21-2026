package com.elfak.slagalica.repository;

import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Registracija
    public void registracija(String email, String lozinka, String korisnickoIme,
                             String region, OnSuccessListener onSuccess, OnErrorListener onError) {
        auth.createUserWithEmailAndPassword(email, lozinka)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    // Pošalji verifikacioni email
                    firebaseUser.sendEmailVerification();

                    // Sačuvaj korisnika u Firestore
                    User user = new User(
                            firebaseUser.getUid(),
                            email,
                            korisnickoIme,
                            region
                    );

                    db.collection("users")
                            .document(firebaseUser.getUid())
                            .set(user)
                            .addOnSuccessListener(unused -> onSuccess.onSuccess())
                            .addOnFailureListener(e -> onError.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    // Logovanje
    public void logovanje(String emailIliKorisnickoIme, String lozinka,
                          OnSuccessListener onSuccess, OnErrorListener onError) {

        // Provjeri da li je email ili korisničko ime
        if (emailIliKorisnickoIme.contains("@")) {
            // Logovanje sa emailom
            loginSaEmailom(emailIliKorisnickoIme, lozinka, onSuccess, onError);
        } else {
            // Logovanje sa korisničkim imenom — pronađi email u Firestore
            db.collection("users")
                    .whereEqualTo("korisnickoIme", emailIliKorisnickoIme)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            onError.onError("Korisničko ime nije pronađeno!");
                            return;
                        }
                        String email = query.getDocuments().get(0).getString("email");
                        loginSaEmailom(email, lozinka, onSuccess, onError);
                    })
                    .addOnFailureListener(e -> onError.onError(e.getMessage()));
        }
    }

    private void loginSaEmailom(String email, String lozinka,
                                OnSuccessListener onSuccess, OnErrorListener onError) {
        auth.signInWithEmailAndPassword(email, lozinka)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    if (!firebaseUser.isEmailVerified()) {
                        onError.onError("Email nije verifikovan! Provjerite inbox.");
                        auth.signOut();
                        return;
                    }

                    onSuccess.onSuccess();
                })
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
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
                .addOnSuccessListener(unused -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    // Interfejsi za callbacks
    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }
}