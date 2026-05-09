package com.elfak.slagalica.fragments.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class NotifikacijeViewModel extends ViewModel {

    public enum Filter { SVE, NEPROCITANE, PROCITANE }

    private final MutableLiveData<List<Notifikacija>> sveNotifikacije = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Filter> aktivniFilter = new MutableLiveData<>(Filter.SVE);
    private final MutableLiveData<List<Notifikacija>> filtrirane = new MutableLiveData<>(new ArrayList<>());

    public NotifikacijeViewModel() {
        List<Notifikacija> uzorci = new ArrayList<>();
        uzorci.add(new Notifikacija("🏆", "Rang lista", "Nedeljna rang lista je završena. Tvoj plasman: 5. mesto.", "06.05.2026 20:00", false));
        uzorci.add(new Notifikacija("💬", "Nova poruka", "Korisnik player123 ti je poslao poruku u četu.", "06.05.2026 18:30", false));
        uzorci.add(new Notifikacija("🎮", "Poziv za partiju", "player456 te poziva na prijateljsku partiju.", "05.05.2026 15:00", true));
        uzorci.add(new Notifikacija("⭐", "Nova liga", "Čestitamo! Prešao si u Prvu ligu!", "04.05.2026 12:00", true));
        sveNotifikacije.setValue(uzorci);
        primijeniFilter();
    }

    public LiveData<List<Notifikacija>> getFiltrirane() {
        return filtrirane;
    }

    public LiveData<Filter> getAktivniFilter() {
        return aktivniFilter;
    }

    public void postaviNotifikacije(List<Notifikacija> notifikacije) {
        sveNotifikacije.setValue(notifikacije);
        primijeniFilter();
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
