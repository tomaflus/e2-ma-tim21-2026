package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Partija;
import com.elfak.slagalica.model.StatusPartije;
import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PartijaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration listenerRegistration;

    public interface OnPartijaListener {
        void onPartija(Partija partija);
    }

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }

    // Kreiraj novu partiju i čekaj protivnika
    public void kreirajPartiju(String korisnickoIme,
                               OnPartijaListener onPartija,
                               OnErrorListener onError) {
        String igracId = auth.getCurrentUser().getUid();

        // Provjeri da li ima partija koja čeka
        db.collection("partije")
                .whereEqualTo("status", StatusPartije.CEKANJE.name())
                .whereEqualTo("prijateljska", false)
                .get()
                .addOnSuccessListener(query -> {
                    // Filtriraj partije gdje igrac1 nije trenutni igrač
                    QueryDocumentSnapshot dostupna = null;
                    for (QueryDocumentSnapshot doc : query) {
                        String igrac1Id = doc.getString("igrac1Id");
                        if (igrac1Id != null && !igrac1Id.equals(igracId)) {
                            dostupna = doc;
                            break;
                        }
                    }

                    if (dostupna != null) {
                        // Pridruži se postojećoj partiji
                        pridruziSePartiji(dostupna.getId(), igracId,
                                korisnickoIme, onPartija, onError);
                    } else {
                        // Kreiraj novu partiju i čekaj
                        novaPartija(igracId, korisnickoIme, false,
                                onPartija, onError);
                    }
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    private void novaPartija(String igracId, String korisnickoIme,
                             boolean prijateljska,
                             OnPartijaListener onPartija,
                             OnErrorListener onError) {
        Partija partija = new Partija(igracId, korisnickoIme, prijateljska);

        db.collection("partije")
                .add(partija)
                .addOnSuccessListener(ref -> {
                    partija.setId(ref.getId());
                    // Slušaj promjene na ovoj partiji
                    slušajPartiju(ref.getId(), onPartija, onError);
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    private void pridruziSePartiji(String partijaId, String igracId,
                                   String korisnickoIme,
                                   OnPartijaListener onPartija,
                                   OnErrorListener onError) {
        db.collection("partije").document(partijaId)
                .update(
                        "igrac2Id", igracId,
                        "igrac2Ime", korisnickoIme,
                        "status", StatusPartije.U_TOKU.name()
                )
                .addOnSuccessListener(unused ->
                        slušajPartiju(partijaId, onPartija, onError))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Real-time listener za partiju
    public void slušajPartiju(String partijaId,
                              OnPartijaListener onPartija,
                              OnErrorListener onError) {
        listenerRegistration = db.collection("partije")
                .document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        mainHandler.post(() -> onError.onError(e.getMessage()));
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Partija partija = snapshot.toObject(Partija.class);
                        if (partija != null) {
                            partija.setId(snapshot.getId());
                            mainHandler.post(() -> onPartija.onPartija(partija));
                        }
                    }
                });
    }

    // Ažuriraj bodove u partiji
    public void azurirajBodove(String partijaId, boolean jeIgrac1,
                               int bodovi, OnSuccessListener onSuccess,
                               OnErrorListener onError) {
        String polje = jeIgrac1 ? "bodovi1" : "bodovi2";
        db.collection("partije").document(partijaId)
                .update(polje, bodovi)
                .addOnSuccessListener(unused ->
                        mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Završi partiju
    public void završiPartiju(String partijaId, String pobjednikId,
                              OnSuccessListener onSuccess,
                              OnErrorListener onError) {
        db.collection("partije").document(partijaId)
                .update(
                        "status", StatusPartije.ZAVRSENA.name(),
                        "pobjednik", pobjednikId
                )
                .addOnSuccessListener(unused ->
                        mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Napusti partiju
    public void napustiPartiju(String partijaId, OnSuccessListener onSuccess,
                               OnErrorListener onError) {
        db.collection("partije").document(partijaId)
                .update("status", StatusPartije.NAPUSTENA.name())
                .addOnSuccessListener(unused ->
                        mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Ukloni listener kad više nije potreban
    public void ukloniListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    // Ažuriraj zvezde i tokene korisnika nakon partije
    public void azurirajZvezdeITokene(String userId, int zvezdeDodati,
                                      OnSuccessListener onSuccess,
                                      OnErrorListener onError) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user == null) return;

                        int novaZvezde = Math.max(0, user.getZvezde() + zvezdeDodati);
                        int noviTokeni = user.getTokeni();

                        // 50 zvezda = 1 token
                        int tokeniOdZvezda = novaZvezde / 50;
                        if (tokeniOdZvezda > user.getZvezde() / 50) {
                            noviTokeni += (tokeniOdZvezda - user.getZvezde() / 50);
                        }

                        db.collection("users").document(userId)
                                .update("zvezde", novaZvezde, "tokeni", noviTokeni)
                                .addOnSuccessListener(unused ->
                                        mainHandler.post(onSuccess::onSuccess))
                                .addOnFailureListener(e ->
                                        mainHandler.post(() ->
                                                onError.onError(e.getMessage())));
                    }
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }
}