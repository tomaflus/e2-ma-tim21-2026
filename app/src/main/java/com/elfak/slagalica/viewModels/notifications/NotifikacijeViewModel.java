package com.elfak.slagalica.viewModels.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.elfak.slagalica.model.Notifikacija;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
                .orderBy("datumVrijeme", Query.Direction.DESCENDING)
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
            case "nagrade": return "🏆";
            case "cet": return "💬";
            case "poziv": return "🎮";
            case "liga": return "⭐";
            default: return "🔔";
        }
    }

    public void markirajKaoProcitanu(String uid, String docId) {
        if (uid == null || uid.isEmpty() || docId == null || docId.isEmpty()) return;
        db.collection("users").document(uid)
                .collection("notifikacije").document(docId)
                .update("procitana", true);
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

    private void primijeniFilter() {
        List<Notifikacija> sve = sveNotifikacije.getValue();
        Filter filter = aktivniFilter.getValue();
        if (sve == null) return;
        if (filter == Filter.SVE) {
            filtrirane.setValue(new ArrayList<>(sve));
            return;
        }
        List<Notifikacija> rezultat = new ArrayList<>();
        for (Notifikacija n : sve) {
            if (filter == Filter.NEPROCITANE && !n.procitana) rezultat.add(n);
            else if (filter == Filter.PROCITANE && n.procitana) rezultat.add(n);
        }
        filtrirane.setValue(rezultat);
    }
}
