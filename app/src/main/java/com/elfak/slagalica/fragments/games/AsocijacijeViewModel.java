package com.elfak.slagalica.fragments.games;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AsocijacijeViewModel extends ViewModel {
    public final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    public final MutableLiveData<String> timerText = new MutableLiveData<>("2:00");
    // koja su polja otvorena: [kolona 0-3][red 0-3]
    public final MutableLiveData<boolean[][]> otvorenaPolja = new MutableLiveData<>(new boolean[4][4]);
    public final MutableLiveData<boolean[]> rijeseneKolone = new MutableLiveData<>(new boolean[4]);
    public final MutableLiveData<Boolean> konacnoRijeseno = new MutableLiveData<>(false);
}
