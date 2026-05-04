package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Poruka;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import com.elfak.slagalica.helpers.NotifikacijaHelper;

public class CetRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ListenerRegistration listenerRegistration;

    public interface OnPorukeListener {
        void onPoruke(List<Poruka> poruke);
    }

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(String poruka);
    }

    // Pošalji poruku
    public void pošaljiPoruku(String tekst, String posiljacIme,
                              String region, OnSuccessListener onSuccess,
                              OnErrorListener onError) {
        String uid = auth.getCurrentUser().getUid();

        Poruka poruka = new Poruka(uid, posiljacIme, tekst, region);

        db.collection("chat")
                .document(region)
                .collection("poruke")
                .add(poruka)
                .addOnSuccessListener(ref ->
                        mainHandler.post(onSuccess::onSuccess))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onError.onError(e.getMessage())));
    }

    // Slušaj poruke u realnom vremenu
    public void slušajPoruke(String region, Context context,
                             OnPorukeListener onPoruke,
                             OnErrorListener onError) {
        String mojId = auth.getCurrentUser().getUid();
        int[] prethodnaVelicina = {-1};

        listenerRegistration = db.collection("chat")
                .document(region)
                .collection("poruke")
                .orderBy("vrijemeSlanja", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        mainHandler.post(() -> onError.onError(e.getMessage()));
                        return;
                    }

                    if (snapshot != null) {
                        List<Poruka> poruke = new ArrayList<>();
                        for (var doc : snapshot.getDocuments()) {
                            Poruka poruka = doc.toObject(Poruka.class);
                            if (poruka != null) {
                                poruka.setId(doc.getId());
                                poruke.add(poruka);
                            }
                        }

                        // Prikaži notifikaciju za novu poruku
                        if (prethodnaVelicina[0] != -1 &&
                                poruke.size() > prethodnaVelicina[0]) {
                            Poruka novaPoruka = poruke.get(poruke.size() - 1);
                            // Samo ako nije moja poruka
                            if (!novaPoruka.getPosiljacId().equals(mojId)) {
                                mainHandler.post(() ->
                                        NotifikacijaHelper.prikaziNotifikacijuCet(
                                                context,
                                                novaPoruka.getPosiljacIme(),
                                                novaPoruka.getTekst()));
                            }
                        }
                        prethodnaVelicina[0] = poruke.size();

                        mainHandler.post(() -> onPoruke.onPoruke(poruke));
                    }
                });
    }

    // Ukloni listener
    public void ukloniListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}