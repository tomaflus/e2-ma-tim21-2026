package com.elfak.slagalica.fragments.games;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SkockoViewModel extends ViewModel {
    public final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    public final MutableLiveData<String> timerText = new MutableLiveData<>("0:30");
    // trenutni unos: 4 slota, svaki sadrži simbol ili null
    public final MutableLiveData<String[]> trenutniUnos = new MutableLiveData<>(new String[4]);
    public final MutableLiveData<Integer> brojPokusaja = new MutableLiveData<>(0);
}
