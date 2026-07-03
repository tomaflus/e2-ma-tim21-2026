package com.elfak.slagalica.viewModels.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.elfak.slagalica.model.Notifikacija;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotifikacijeViewModel extends ViewModel {

    public enum Filter { SVE, NEPROCITANE, PROCITANE }

    private final MutableLiveData<List<Notifikacija>> sveNotifikacije = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Filter> aktivniFilter = new MutableLiveData<>(Filter.SVE);
    private final MutableLiveData<List<Notifikacija>> filtrirane = new MutableLiveData<>(new ArrayList<>());

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public NotifikacijeViewModel() {
        ucitajIzFirestore();
    }

    private void ucitajIzFirestore() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .collection("notifikacije")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<Notifikacija> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Notifikacija n = doc.toObject(Notifikacija.class);
                        n.dokumentId = doc.getId();
                        n.ikona = ikonaZaTip(n.tip, n.ikona);
                        lista.add(n);
                    }
                    sveNotifikacije.setValue(lista);
                    primijeniFilter();
                });
    }

    private String ikonaZaTip(String tip, String postojecaIkona) {
        if (postojecaIkona != null && !postojecaIkona.isEmpty()) return postojecaIkona;
        if (tip == null) return "🔔";
        switch (tip) {
            case "nagrade": return "⭐";
            case "cet": return "💬";
            case "poziv": return "🎮";
            case "liga": return "⭐";
            case "misije": return "✅";
            default: return "🔔";
        }
    }

    public void markirajKaoProcitanu(String uid, String docId) {
        if (uid == null || uid.isEmpty() || docId == null || docId.isEmpty()) return;
        db.collection("users").document(uid)
                .collection("notifikacije").document(docId)
                .update("procitana", true);
    }

    public void obrisiNotifikacije(String uid, List<String> docIds) {
        if (uid == null || uid.isEmpty() || docIds == null || docIds.isEmpty()) return;
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (String docId : docIds) {
            batch.delete(db.collection("users").document(uid)
                    .collection("notifikacije").document(docId));
        }
        batch.commit();
    }

    public LiveData<List<Notifikacija>> getFiltrirane() {
        return filtrirane;
    }

    public LiveData<Filter> getAktivniFilter() {
        return aktivniFilter;
    }

    public void postaviFilter(Filter filter) {
        aktivniFilter.setValue(filter);
        primijeniFilter();
    }

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    private void sortDescByTime(List<Notifikacija> lista) {
        lista.sort((a, b) -> {
            try {
                Date da = DATE_FMT.parse(a.datumVrijeme != null ? a.datumVrijeme : "");
                Date db2 = DATE_FMT.parse(b.datumVrijeme != null ? b.datumVrijeme : "");
                if (da == null || db2 == null) return 0;
                return db2.compareTo(da);
            } catch (ParseException e) {
                return 0;
            }
        });
    }

    private void primijeniFilter() {
        List<Notifikacija> sve = sveNotifikacije.getValue();
        Filter filter = aktivniFilter.getValue();
        if (sve == null) return;

        if (filter == Filter.SVE) {
            List<Notifikacija> neprocitane = new ArrayList<>();
            List<Notifikacija> procitane = new ArrayList<>();
            for (Notifikacija n : sve) {
                if (!n.procitana) neprocitane.add(n);
                else procitane.add(n);
            }
            sortDescByTime(neprocitane);
            sortDescByTime(procitane);
            List<Notifikacija> rezultat = new ArrayList<>(neprocitane);
            rezultat.addAll(procitane);
            filtrirane.setValue(rezultat);
            return;
        }

        List<Notifikacija> rezultat = new ArrayList<>();
        for (Notifikacija n : sve) {
            if (filter == Filter.NEPROCITANE && !n.procitana) rezultat.add(n);
            else if (filter == Filter.PROCITANE && n.procitana) rezultat.add(n);
        }
        sortDescByTime(rezultat);
        filtrirane.setValue(rezultat);
    }
}
