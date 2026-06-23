package com.elfak.slagalica.repository;

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RangRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnRangLoaded {
        void onLoaded(List<User> lista);
    }

    public void dohvatiNedeljnuRang(String ciklusId, OnRangLoaded onLoaded) {
        db.collection("users")
                .whereEqualTo("nedeljaCiklusId", ciklusId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User u = doc.toObject(User.class);
                        u.setId(doc.getId());
                        if (u.isRangiranNedelja()) lista.add(u);
                    }
                    Collections.sort(lista, (a, b) -> b.getNedeljneZvezde() - a.getNedeljneZvezde());
                    if (lista.size() > 10) lista = lista.subList(0, 10);
                    List<User> finalLista = lista;
                    mainHandler.post(() -> onLoaded.onLoaded(finalLista));
                })
                .addOnFailureListener(e -> mainHandler.post(() -> onLoaded.onLoaded(new ArrayList<>())));
    }

    public void dohvatiMesecnuRang(String ciklusId, OnRangLoaded onLoaded) {
        db.collection("users")
                .whereEqualTo("mesecCiklusId", ciklusId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User u = doc.toObject(User.class);
                        u.setId(doc.getId());
                        if (u.isRangiranMesec()) lista.add(u);
                    }
                    Collections.sort(lista, (a, b) -> b.getMesecneZvezde() - a.getMesecneZvezde());
                    if (lista.size() > 10) lista = lista.subList(0, 10);
                    List<User> finalLista = lista;
                    mainHandler.post(() -> onLoaded.onLoaded(finalLista));
                })
                .addOnFailureListener(e -> mainHandler.post(() -> onLoaded.onLoaded(new ArrayList<>())));
    }
}
