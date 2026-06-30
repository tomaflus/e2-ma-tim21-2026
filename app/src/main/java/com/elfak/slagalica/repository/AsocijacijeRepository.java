package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Asocijacija;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        dohvatiNasumicnoPitanjeIzuzimajuci(null, onUcitano, onGreska);
    }

    public void dohvatiNasumicnoPitanjeIzuzimajuci(String excludeId,
                                                    OnUcitanoListener onUcitano,
                                                    OnGreskaListener onGreska) {
        db.collection("asocijacije")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        seedujIVrati(onUcitano, onGreska);
                        return;
                    }

                    List<QueryDocumentSnapshot> svi = new ArrayList<>();
                    List<QueryDocumentSnapshot> filtrirani = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        svi.add(doc);
                        if (!doc.getId().equals(excludeId)) filtrirani.add(doc);
                    }

                    List<QueryDocumentSnapshot> kandidati = filtrirani.isEmpty() ? svi : filtrirani;
                    QueryDocumentSnapshot nasumicni =
                            kandidati.get(new Random().nextInt(kandidati.size()));
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

    // Upiši 3 hardkodovana pitanja u Firebase, pa vrati jedno
    private void seedujIVrati(OnUcitanoListener onUcitano, OnGreskaListener onGreska) {
        List<Map<String, Object>> pitanja = napraviHardkodovanaPitanja();

        WriteBatch batch = db.batch();
        List<com.google.firebase.firestore.DocumentReference> refs = new ArrayList<>();
        for (Map<String, Object> p : pitanja) {
            com.google.firebase.firestore.DocumentReference ref =
                    db.collection("asocijacije").document();
            batch.set(ref, p);
            refs.add(ref);
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    // Uzmi prvo pitanje (ili nasumično)
                    com.google.firebase.firestore.DocumentReference izabrani =
                            refs.get(new Random().nextInt(refs.size()));
                    izabrani.get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    mainHandler.post(() -> onGreska.onGreska("Greška pri seed-u!"));
                                    return;
                                }
                                Asocijacija pitanje = doc.toObject(Asocijacija.class);
                                pitanje.setId(doc.getId());
                                mainHandler.post(() -> onUcitano.onUcitano(pitanje));
                            })
                            .addOnFailureListener(e ->
                                    mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    private List<Map<String, Object>> napraviHardkodovanaPitanja() {
        List<Map<String, Object>> lista = new ArrayList<>();

        // Tema: ZEMLJA (planeta)
        Map<String, Object> p1 = new HashMap<>();
        p1.put("poljeA1", "Azija"); p1.put("poljeA2", "Afrika");
        p1.put("poljeA3", "Amerika"); p1.put("poljeA4", "Australija");
        p1.put("poljeB1", "Tihi"); p1.put("poljeB2", "Atlantski");
        p1.put("poljeB3", "Indijski"); p1.put("poljeB4", "Arktički");
        p1.put("poljeC1", "Himalaji"); p1.put("poljeC2", "Andi");
        p1.put("poljeC3", "Alpi"); p1.put("poljeC4", "Kavkaz");
        p1.put("poljeD1", "Nil"); p1.put("poljeD2", "Amazon");
        p1.put("poljeD3", "Dunav"); p1.put("poljeD4", "Tisa");
        p1.put("rjesenjeA", "Kontinenti"); p1.put("rjesenjeB", "Okeani");
        p1.put("rjesenjeC", "Planine"); p1.put("rjesenjeD", "Reke");
        p1.put("konacnoRjesenje", "Zemlja");
        lista.add(p1);

        // Tema: HRANA
        Map<String, Object> p2 = new HashMap<>();
        p2.put("poljeA1", "Jabuka"); p2.put("poljeA2", "Kruška");
        p2.put("poljeA3", "Šljiva"); p2.put("poljeA4", "Trešnja");
        p2.put("poljeB1", "Paradajz"); p2.put("poljeB2", "Paprika");
        p2.put("poljeB3", "Krastavac"); p2.put("poljeB4", "Tikvica");
        p2.put("poljeC1", "Pšenica"); p2.put("poljeC2", "Kukuruz");
        p2.put("poljeC3", "Ječam"); p2.put("poljeC4", "Raž");
        p2.put("poljeD1", "Mleko"); p2.put("poljeD2", "Sir");
        p2.put("poljeD3", "Jogurt"); p2.put("poljeD4", "Kajmak");
        p2.put("rjesenjeA", "Voće"); p2.put("rjesenjeB", "Povrće");
        p2.put("rjesenjeC", "Žitarice"); p2.put("rjesenjeD", "Mlečni proizvodi");
        p2.put("konacnoRjesenje", "Hrana");
        lista.add(p2);

        // Tema: MUZIKA
        Map<String, Object> p3 = new HashMap<>();
        p3.put("poljeA1", "Gitara"); p3.put("poljeA2", "Bubnjevi");
        p3.put("poljeA3", "Klavir"); p3.put("poljeA4", "Violina");
        p3.put("poljeB1", "Bach"); p3.put("poljeB2", "Betoven");
        p3.put("poljeB3", "Mocart"); p3.put("poljeB4", "Šopen");
        p3.put("poljeC1", "Pop"); p3.put("poljeC2", "Rok");
        p3.put("poljeC3", "Džez"); p3.put("poljeC4", "Klasika");
        p3.put("poljeD1", "Do"); p3.put("poljeD2", "Re");
        p3.put("poljeD3", "Mi"); p3.put("poljeD4", "Fa");
        p3.put("rjesenjeA", "Instrumenti"); p3.put("rjesenjeB", "Kompozitori");
        p3.put("rjesenjeC", "Žanrovi"); p3.put("rjesenjeD", "Note");
        p3.put("konacnoRjesenje", "Muzika");
        lista.add(p3);

        return lista;
    }
}
