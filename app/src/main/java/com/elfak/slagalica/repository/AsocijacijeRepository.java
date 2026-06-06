package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Asocijacija;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AsocijacijeRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnUcitanoListener {
        void onUcitano(Asocijacija pitanje);
    }

    public interface OnGreskaListener {
        void onGreska(String poruka);
    }

    public void dohvatiNasumicnoPitanje(OnUcitanoListener onUcitano,
                                         OnGreskaListener onGreska) {
        db.collection("asocijacije")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        mainHandler.post(() ->
                                onGreska.onGreska("Nema asocijacija u bazi!"));
                        return;
                    }

                    List<QueryDocumentSnapshot> dokumenti = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        dokumenti.add(doc);
                    }

                    Random random = new Random();
                    QueryDocumentSnapshot nasumicni =
                            dokumenti.get(random.nextInt(dokumenti.size()));

                    Asocijacija pitanje = nasumicni.toObject(Asocijacija.class);
                    pitanje.setId(nasumicni.getId());

                    mainHandler.post(() -> onUcitano.onUcitano(pitanje));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    public void dohvatiPitanjePoId(String id, OnUcitanoListener onUcitano,
                                    OnGreskaListener onGreska) {
        db.collection("asocijacije").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        mainHandler.post(() -> onGreska.onGreska("Pitanje nije pronađeno!"));
                        return;
                    }
                    Asocijacija pitanje = doc.toObject(Asocijacija.class);
                    pitanje.setId(doc.getId());
                    mainHandler.post(() -> onUcitano.onUcitano(pitanje));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }
}
