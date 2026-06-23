package com.elfak.slagalica.viewModels.games;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Random;

public class SkockoViewModel extends ViewModel {

    public static final String[] SIMBOLI = {"😀", "🟥", "🔵", "❤️", "🔺", "⭐"};

    public enum Faza { IGRAC, PROTIVNIK, ZAVRSENO }

    // LiveData samo za score (observe iz fragmenta)
    public final MutableLiveData<Integer> score = new MutableLiveData<>(0);

    // Svo stanje igre — plain polja, preživljavaju rotaciju u ViewModel-u
    public final String[][] tabla = new String[6][4]; // null = prazno
    public final String[] feedbackovi = new String[6]; // null = nije submitovan
    public String[] tajnaKombinacija = null;
    public String[] rjesenje = null; // null = nije prikazano

    public int aktivniRed = 0;
    public int aktivniSlot = 0;
    public Faza faza = Faza.IGRAC;

    // Epoch ms kada ističe tajmer aktivne faze; 0 = nije pokrenut
    public long timerEndMs = 0;

    public void inicijalizujTajnu() {
        if (tajnaKombinacija != null) return;
        Random rnd = new Random();
        tajnaKombinacija = new String[4];
        for (int i = 0; i < 4; i++) {
            tajnaKombinacija[i] = SIMBOLI[rnd.nextInt(SIMBOLI.length)];
        }
    }
}
