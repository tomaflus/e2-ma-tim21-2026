package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.KorakPoKorak;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KorakPoKorakRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnUcitanoListener {
        void onUcitano(KorakPoKorak pitanje);
    }

    public interface OnGreskaListener {
        void onGreska(String poruka);
    }

    // Dohvati nasumično pitanje iz baze
    public void dohvatiNasumicnoPitanje(OnUcitanoListener onUcitano,
                                        OnGreskaListener onGreska) {
        db.collection("korakPoKorak")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        mainHandler.post(() ->
                                onGreska.onGreska("Nema pitanja u bazi!"));
                        return;
                    }

                    // Odaberi nasumično pitanje
                    List<QueryDocumentSnapshot> dokumenti = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        dokumenti.add(doc);
                    }

                    Random random = new Random();
                    QueryDocumentSnapshot nasumicni =
                            dokumenti.get(random.nextInt(dokumenti.size()));

                    KorakPoKorak pitanje = nasumicni.toObject(KorakPoKorak.class);
                    pitanje.setId(nasumicni.getId());

                    mainHandler.post(() -> onUcitano.onUcitano(pitanje));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }
}