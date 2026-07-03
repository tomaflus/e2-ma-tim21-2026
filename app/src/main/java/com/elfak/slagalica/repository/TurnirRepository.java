package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.model.Turnir;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TurnirRepository {

    private static final String KOLEKCIJA = "turniri";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnUspjehListener { void onUspjeh(); }
    public interface OnGreskaListener { void onGreska(String poruka); }
    public interface OnTurniriListener { void onChange(List<Turnir> svi); }

    // ── Napravi turnir ────────────────────────────────────────────────────────

    public void napraviTurnir(String naziv, String vlasnikId, String vlasnikIme,
                              int vlasnikLiga, String vlasnikAvatarUrl,
                              OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        Map<String, Object> igrac = igracMap(vlasnikId, vlasnikIme, vlasnikLiga, vlasnikAvatarUrl);

        List<Map<String, Object>> igraci = new ArrayList<>();
        igraci.add(igrac);

        Map<String, Object> doc = new HashMap<>();
        doc.put("naziv", naziv);
        doc.put("vlasnikId", vlasnikId);
        doc.put("vlasnikIme", vlasnikIme);
        doc.put("igraci", igraci);
        doc.put("status", "CEKA");
        doc.put("createdAt", System.currentTimeMillis());

        db.collection(KOLEKCIJA).add(doc)
                .addOnSuccessListener(ref -> mainHandler.post(onUspjeh::onUspjeh))
                .addOnFailureListener(e -> mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Slusaj sve turnire (real-time) ────────────────────────────────────────

    public ListenerRegistration slusajTurnire(OnTurniriListener listener) {
        return db.collection(KOLEKCIJA)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((query, e) -> {
                    if (e != null || query == null) return;
                    List<Turnir> lista = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        Turnir t = doc.toObject(Turnir.class);
                        t.setId(doc.getId());
                        lista.add(t);
                    }
                    mainHandler.post(() -> listener.onChange(lista));
                });
    }

    // ── Prijavi se ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void prijaviSe(String turnirId, String naziv, String vlasnikId,
                          String myId, String myIme, int myLiga, String myAvatarUrl,
                          OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        Map<String, Object> igracMap = igracMap(myId, myIme, myLiga, myAvatarUrl);
        com.google.firebase.firestore.DocumentReference ref = db.collection(KOLEKCIJA).document(turnirId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Integer>) tx -> {
            com.google.firebase.firestore.DocumentSnapshot snap = tx.get(ref);
            if (!"CEKA".equals(snap.getString("status"))) return -1;

            List<Map<String, Object>> igraci = (List<Map<String, Object>>) snap.get("igraci");
            if (igraci == null) igraci = new ArrayList<>();
            if (igraci.size() >= 4) return -1;

            for (Map<String, Object> m : igraci) {
                if (myId.equals(m.get("id"))) return -1;
            }

            igraci = new ArrayList<>(igraci);
            igraci.add(igracMap);

            Map<String, Object> upd = new HashMap<>();
            upd.put("igraci", igraci);
            if (igraci.size() == 4) upd.put("status", "U_TOKU");
            tx.update(ref, upd);
            return igraci.size();

        }).addOnSuccessListener(newSize -> {
            if (newSize == null || newSize < 0) {
                mainHandler.post(() -> onGreska.onGreska("Nije moguće pridružiti se turniru."));
                return;
            }

            if (!myId.equals(vlasnikId)) {
                posaljiNotifikaciju(vlasnikId, "🏆", "Novi igrač!",
                        "Novi igrac se pridruzio turniru '" + naziv + "'!");
            }

            if (newSize == 4) {
                ref.get().addOnSuccessListener(snap -> {
                    List<Map<String, Object>> igraci =
                            (List<Map<String, Object>>) snap.get("igraci");
                    if (igraci == null || igraci.size() < 4) return;

                    // Pronađi vlasnika i ostale igrače
                    Map<String, Object> vlasnikMap = null;
                    List<Map<String, Object>> ostali = new ArrayList<>();
                    for (Map<String, Object> m : igraci) {
                        if (vlasnikId.equals(m.get("id"))) vlasnikMap = m;
                        else ostali.add(m);
                    }
                    if (vlasnikMap == null || ostali.size() < 3) return;

                    // Nasumični zreb: vlasnik sa nasumičnim igračem, preostala dva čine drugi par
                    Collections.shuffle(ostali);
                    Map<String, Object> sf1i1 = vlasnikMap;
                    Map<String, Object> sf1i2 = ostali.get(0);
                    Map<String, Object> sf2i1 = ostali.get(1);
                    Map<String, Object> sf2i2 = ostali.get(2);

                    WriteBatch batch = db.batch();

                    // Tokeni i notifikacije za sve igrače
                    for (Map<String, Object> m : igraci) {
                        String id = (String) m.get("id");
                        batch.update(db.collection("users").document(id),
                                "tokeni", FieldValue.increment(-3));
                        posaljiNotifikaciju(id, "🏆", "Turnir počinje!",
                                "Turnir '" + naziv + "' ima 4 igraca i pocinje!");
                    }

                    // Upis zreba u Firestore
                    Map<String, Object> zrebPolja = new HashMap<>();
                    zrebPolja.put("sf1Igrac1", sf1i1);
                    zrebPolja.put("sf1Igrac2", sf1i2);
                    zrebPolja.put("sf2Igrac1", sf2i1);
                    zrebPolja.put("sf2Igrac2", sf2i2);
                    batch.update(ref, zrebPolja);

                    batch.commit();
                });
            }

            mainHandler.post(onUspjeh::onUspjeh);
        }).addOnFailureListener(e ->
                mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Odjavi se ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void odjavi(String turnirId, String naziv, String vlasnikId,
                       String myId, String myIme,
                       OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        com.google.firebase.firestore.DocumentReference ref = db.collection(KOLEKCIJA).document(turnirId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) tx -> {
            com.google.firebase.firestore.DocumentSnapshot snap = tx.get(ref);
            List<Map<String, Object>> igraci = (List<Map<String, Object>>) snap.get("igraci");
            if (igraci == null) return null;

            List<Map<String, Object>> novi = new ArrayList<>();
            for (Map<String, Object> m : igraci) {
                if (!myId.equals(m.get("id"))) novi.add(m);
            }
            tx.update(ref, "igraci", novi);
            return null;
        }).addOnSuccessListener(v -> {
            if (!myId.equals(vlasnikId)) {
                posaljiNotifikaciju(vlasnikId, "❌", "Igrač napustio turnir!",
                        "Igrac " + myIme + " napustio turnir '" + naziv + "'!");
            }
            mainHandler.post(onUspjeh::onUspjeh);
        }).addOnFailureListener(e ->
                mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Obriši turnir (samo vlasnik) ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void obrisi(String turnirId, String naziv, String vlasnikId,
                       OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        com.google.firebase.firestore.DocumentReference ref = db.collection(KOLEKCIJA).document(turnirId);

        ref.get().addOnSuccessListener(snap -> {
            List<Map<String, Object>> igraci = (List<Map<String, Object>>) snap.get("igraci");

            ref.delete().addOnSuccessListener(v -> {
                if (igraci != null) {
                    for (Map<String, Object> m : igraci) {
                        String id = (String) m.get("id");
                        if (!vlasnikId.equals(id)) {
                            posaljiNotifikaciju(id, "❌", "Turnir obrisan!",
                                    "Turnir '" + naziv + "' je obrisan!");
                        }
                    }
                }
                mainHandler.post(onUspjeh::onUspjeh);
            }).addOnFailureListener(e ->
                    mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
        }).addOnFailureListener(e ->
                mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Notifikacija helper ───────────────────────────────────────────────────

    public void posaljiNotifikaciju(String targetUserId, String ikona, String naslov, String sadrzaj) {
        String datum = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        Notifikacija n = new Notifikacija(ikona, naslov, sadrzaj, datum, false);
        n.tip = "turnir";
        db.collection("users").document(targetUserId)
                .collection("notifikacije").add(n);
    }

    // ── Ready-up za SF meč ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void readySe(String turnirId, String sfPrefix, boolean jeIgrac1,
                        OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        com.google.firebase.firestore.DocumentReference turnirRef =
                db.collection(KOLEKCIJA).document(turnirId);
        com.google.firebase.firestore.DocumentReference partijaRef =
                db.collection("partije").document(); // pre-generiši ID

        String myReadyKey    = sfPrefix + (jeIgrac1 ? "ReadyIgrac1" : "ReadyIgrac2");
        String otherReadyKey = sfPrefix + (jeIgrac1 ? "ReadyIgrac2" : "ReadyIgrac1");
        String partijaIdKey  = sfPrefix + "PartijaId";

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) tx -> {
            com.google.firebase.firestore.DocumentSnapshot snap = tx.get(turnirRef);

            if (snap.getString(partijaIdKey) != null) return null; // već kreirana

            boolean otherReady = Boolean.TRUE.equals(snap.getBoolean(otherReadyKey));

            Map<String, Object> upd = new HashMap<>();
            upd.put(myReadyKey, true);
            tx.update(turnirRef, upd);

            if (otherReady) {
                Map<String, Object> sfIgrac1Map = (Map<String, Object>) snap.get(sfPrefix + "Igrac1");
                Map<String, Object> sfIgrac2Map = (Map<String, Object>) snap.get(sfPrefix + "Igrac2");
                if (sfIgrac1Map == null || sfIgrac2Map == null) return null;

                Map<String, Object> partija = new HashMap<>();
                partija.put("igrac1Id",  sfIgrac1Map.get("id"));
                partija.put("igrac1Ime", sfIgrac1Map.get("ime"));
                partija.put("igrac2Id",  sfIgrac2Map.get("id"));
                partija.put("igrac2Ime", sfIgrac2Map.get("ime"));
                partija.put("status",       "U_TOKU");
                partija.put("trenutnaIgra", 0);
                partija.put("bodovi1",      0);
                partija.put("bodovi2",      0);
                partija.put("prijateljska", false);
                partija.put("kreirano",     System.currentTimeMillis());

                tx.set(partijaRef, partija);
                tx.update(turnirRef, partijaIdKey, partijaRef.getId());
            }
            return null;
        }).addOnSuccessListener(v -> mainHandler.post(onUspjeh::onUspjeh))
          .addOnFailureListener(e -> mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Upiši rezultat SF/Final/Trece meča u turnirski dokument ──────────────

    @SuppressWarnings("unchecked")
    public void azurirajRezultatSF(String turnirId, String sfPrefix, int bodovi1, int bodovi2,
                                   OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        com.google.firebase.firestore.DocumentReference ref = db.collection(KOLEKCIJA).document(turnirId);
        String bodovi1Key = sfPrefix + "Bodovi1";

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Boolean>) tx -> {
            com.google.firebase.firestore.DocumentSnapshot snap = tx.get(ref);
            if (snap.get(bodovi1Key) != null) return false; // već upisano

            Map<String, Object> igrac1Map = (Map<String, Object>) snap.get(sfPrefix + "Igrac1");
            Map<String, Object> igrac2Map = (Map<String, Object>) snap.get(sfPrefix + "Igrac2");
            if (igrac1Map == null || igrac2Map == null) return false;

            Map<String, Object> pobjednik = bodovi1 >= bodovi2 ? igrac1Map : igrac2Map;
            Map<String, Object> gubitnik  = bodovi1 >= bodovi2 ? igrac2Map : igrac1Map;

            Map<String, Object> upd = new HashMap<>();
            upd.put(bodovi1Key, (long) bodovi1);
            upd.put(sfPrefix + "Bodovi2", (long) bodovi2);

            boolean turnirGotov = false;
            if ("sf1".equals(sfPrefix) || "sf2".equals(sfPrefix)) {
                String slot = "sf1".equals(sfPrefix) ? "1" : "2";
                upd.put("finalIgrac" + slot, pobjednik);
                upd.put("treceIgrac" + slot, gubitnik);
            } else if ("final".equals(sfPrefix)) {
                upd.put("pobjednikIme", pobjednik.get("ime"));
                upd.put("pobjednikId",  pobjednik.get("id"));
                upd.put("drugiIme",     gubitnik.get("ime"));
                upd.put("drugiId",      gubitnik.get("id"));
                if (snap.get("treceBodovi1") != null) { // trece već gotov
                    upd.put("status", "GOTOV");
                    turnirGotov = true;
                }
            } else if ("trece".equals(sfPrefix)) {
                upd.put("treciIme",    pobjednik.get("ime"));
                upd.put("treciId",     pobjednik.get("id"));
                upd.put("cetvrtiIme",  gubitnik.get("ime"));
                upd.put("cetvrtiId",   gubitnik.get("id"));
                if (snap.get("finalBodovi1") != null) { // finale već gotov
                    upd.put("status", "GOTOV");
                    turnirGotov = true;
                }
            }

            tx.update(ref, upd);
            return turnirGotov;
        }).addOnSuccessListener(gotov -> {
            if (Boolean.TRUE.equals(gotov)) {
                posaljiNotifikacijeZavrsetka(turnirId);
            }
            mainHandler.post(onUspjeh::onUspjeh);
        }).addOnFailureListener(e -> mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    private void posaljiNotifikacijeZavrsetka(String turnirId) {
        db.collection(KOLEKCIJA).document(turnirId).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;
            String naziv   = snap.getString("naziv") != null ? snap.getString("naziv") : "Turnir";
            String datum   = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                 .format(new java.util.Date());

            String[] ids  = {
                (String) snap.getString("pobjednikId"),
                (String) snap.getString("drugiId"),
                (String) snap.getString("treciId"),
                (String) snap.getString("cetvrtiId")
            };
            String[] mjesta = { "1", "2", "3", "4" };

            for (int i = 0; i < ids.length; i++) {
                String uid = ids[i];
                if (uid == null) continue;
                String sadrzaj = "Turnir '" + naziv + "' je završen! Osvojili ste " + mjesta[i] + ". mjesto.";
                com.elfak.slagalica.model.Notifikacija n =
                        new com.elfak.slagalica.model.Notifikacija("🏆", "Turnir završen!", sadrzaj, datum, false);
                n.tip      = "turnir";
                n.turnirId = turnirId;
                db.collection("users").document(uid).collection("notifikacije").add(n);
            }
        });
    }

    // ── Odustani od čekanja SF meča (resetuj ready flag) ─────────────────────

    public void odustaniOdCekanja(String turnirId, String sfPrefix, boolean jeIgrac1,
                                  OnUspjehListener onUspjeh, OnGreskaListener onGreska) {
        String myReadyKey   = sfPrefix + (jeIgrac1 ? "ReadyIgrac1" : "ReadyIgrac2");
        String partijaIdKey = sfPrefix + "PartijaId";
        com.google.firebase.firestore.DocumentReference ref = db.collection(KOLEKCIJA).document(turnirId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) tx -> {
            com.google.firebase.firestore.DocumentSnapshot snap = tx.get(ref);
            if (snap.getString(partijaIdKey) != null) return null; // partija već kreirana
            tx.update(ref, myReadyKey, false);
            return null;
        }).addOnSuccessListener(v -> mainHandler.post(onUspjeh::onUspjeh))
          .addOnFailureListener(e -> mainHandler.post(() -> onGreska.onGreska(e.getMessage())));
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private Map<String, Object> igracMap(String id, String ime, int liga, String avatarUrl) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("ime", ime);
        m.put("liga", (long) liga);
        if (avatarUrl != null && !avatarUrl.isEmpty()) m.put("avatarUrl", avatarUrl);
        return m;
    }
}
