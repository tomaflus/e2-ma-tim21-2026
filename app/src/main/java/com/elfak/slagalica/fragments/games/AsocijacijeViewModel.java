package com.elfak.slagalica.fragments.games;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AsocijacijeViewModel extends ViewModel {

    // Hardkodovana asocijacija — tema: ZEMLJA (planeta)
    public static final String[][] POLJA = {
        {"Azija", "Afrika", "Amerika", "Australija"}, // Kolona A
        {"Tihi", "Atlantski", "Indijski", "Arktički"}, // Kolona B
        {"Himalaji", "Ande", "Alpe", "Kavkaz"},        // Kolona C
        {"Nil", "Amazon", "Dunav", "Jangce"}            // Kolona D
    };
    public static final String[] RJESENJA_KOLONA = {"Kontinenti", "Okeani", "Planine", "Reke"};
    public static final String KONACNO_RJESENJE = "Zemlja";
    public static final char[] SLOVA_KOLONA = {'A', 'B', 'C', 'D'};

    public final MutableLiveData<Integer> score = new MutableLiveData<>(0);

    // Stanje igre — plain polja preživljavaju rotaciju u ViewModel-u
    public final boolean[][] otvorenaPolja = new boolean[4][4];
    public final boolean[] pogodenaKolona = new boolean[4];
    public boolean konacnoPogodeno = false;
    public boolean zavrsen = false;
    public long timerEndMs = 0;
}
