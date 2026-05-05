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

    public LiveData<List<Notifikacija>> getFiltrirane() {
        return filtrirane;
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
