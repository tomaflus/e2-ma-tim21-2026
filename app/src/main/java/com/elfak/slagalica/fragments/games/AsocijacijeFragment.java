package com.elfak.slagalica.fragments.games;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentAsocijacijeBinding;

public class AsocijacijeFragment extends Fragment {

    private static final long TRAJANJE = 120_000;

    private static final int BOJA_SKRIVENO = Color.parseColor("#A8D0EC");
    private static final int BOJA_OTKRIVENO = Color.parseColor("#C0392B");
    private static final int BOJA_TEKST_TAMNO = Color.parseColor("#12205A");

    private FragmentAsocijacijeBinding binding;
    private AsocijacijeViewModel viewModel;
    private CountDownTimer tajmer;

    private com.google.android.material.button.MaterialButton[][] itemDugmad;
    private com.google.android.material.button.MaterialButton[] rezDugmad;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAsocijacijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);
        viewModel.score.observe(getViewLifecycleOwner(),
                s -> binding.tvBodovi.setText("Bodovi: " + s));

        inicijalizujDugmad();
        obnavljanjeUI();
        obnavljanjeTajmera();
    }

    private void inicijalizujDugmad() {
        // Gornje kolone (A, B) prikazane su 4→1 odozgo, pa mapiramo index 0→btnA4 itd.
        // Logika igre i dalje koristi POLJA[k][r] gdje r=0 je A1, r=3 je A4.
        itemDugmad = new com.google.android.material.button.MaterialButton[][]{
            {binding.btnA1, binding.btnA2, binding.btnA3, binding.btnA4},
            {binding.btnB1, binding.btnB2, binding.btnB3, binding.btnB4},
            {binding.btnC1, binding.btnC2, binding.btnC3, binding.btnC4},
            {binding.btnD1, binding.btnD2, binding.btnD3, binding.btnD4}
        };
        rezDugmad = new com.google.android.material.button.MaterialButton[]{
            binding.btnRezA, binding.btnRezB, binding.btnRezC, binding.btnRezD
        };

        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                final int kolona = k, red = r;
                itemDugmad[k][r].setOnClickListener(v -> {
                    if (!viewModel.zavrsen && !viewModel.otvorenaPolja[kolona][red]) {
                        viewModel.otvorenaPolja[kolona][red] = true;
                        otkriPolje(kolona, red);
                    }
                });
            }
            final int kolona = k;
            rezDugmad[k].setOnClickListener(v -> {
                if (!viewModel.zavrsen && !viewModel.pogodenaKolona[kolona]) {
                    prikaziDialog(kolona);
                }
            });
        }

        binding.btnKonacno.setOnClickListener(v -> {
            if (!viewModel.zavrsen && !viewModel.konacnoPogodeno) {
                prikaziDialog(-1);
            }
        });
    }

    private void obnavljanjeUI() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                if (viewModel.otvorenaPolja[k][r]) otkriPolje(k, r);
            }
            if (viewModel.pogodenaKolona[k]) oznaciPogodenuKolonu(k);
        }
        if (viewModel.konacnoPogodeno) {
            binding.btnKonacno.setText(AsocijacijeViewModel.KONACNO_RJESENJE);
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);
        }
        if (viewModel.zavrsen) onemogucuiInterakciju();
    }

    private void obnavljanjeTajmera() {
        if (viewModel.zavrsen) {
            binding.tvTajmer.setText("⏱ 0:00");
            return;
        }
        long sada = System.currentTimeMillis();
        if (viewModel.timerEndMs == 0) viewModel.timerEndMs = sada + TRAJANJE;
        long preostalo = viewModel.timerEndMs - sada;
        if (preostalo > 0) {
            startTajmer(preostalo);
        } else {
            viewModel.zavrsen = true;
            binding.tvTajmer.setText("⏱ 0:00");
            onemogucuiInterakciju();
        }
    }

    private void startTajmer(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                long sec = ms / 1000;
                binding.tvTajmer.setText(String.format("⏱ %d:%02d", sec / 60, sec % 60));
            }
            @Override public void onFinish() {
                viewModel.zavrsen = true;
                binding.tvTajmer.setText("⏱ 0:00");
                onemogucuiInterakciju();
            }
        }.start();
    }

    private void otkriPolje(int k, int r) {
        itemDugmad[k][r].setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
        itemDugmad[k][r].setTextColor(Color.WHITE);
        itemDugmad[k][r].setText(AsocijacijeViewModel.POLJA[k][r]);
    }

    private void oznaciPogodenuKolonu(int k) {
        rezDugmad[k].setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
        rezDugmad[k].setTextColor(Color.WHITE);
        rezDugmad[k].setText(AsocijacijeViewModel.RJESENJA_KOLONA[k]);
    }

    private void prikaziDialog(int kolona) {
        String naslov = kolona == -1
                ? "Konačno rješenje"
                : "Rješenje kolone " + AsocijacijeViewModel.SLOVA_KOLONA[kolona];

        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("Unesite odgovor...");

        new AlertDialog.Builder(requireContext())
                .setTitle(naslov)
                .setView(et)
                .setPositiveButton("Pogodi", (d, w) ->
                        provjeriOdgovor(kolona, et.getText().toString().trim()))
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void provjeriOdgovor(int kolona, String unos) {
        String tacno = kolona == -1
                ? AsocijacijeViewModel.KONACNO_RJESENJE
                : AsocijacijeViewModel.RJESENJA_KOLONA[kolona];

        if (!unos.equalsIgnoreCase(tacno)) {
            Toast.makeText(getContext(), "Netačno!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (kolona == -1) {
            viewModel.konacnoPogodeno = true;
            viewModel.zavrsen = true;
            if (tajmer != null) tajmer.cancel();
            int bodovi = izracunajBodoveKonacno(); // score se računa prije otkrivanja
            dodajBodove(bodovi);
            // Otkrij sva preostala polja i neodgovorene kolone
            for (int k = 0; k < 4; k++) {
                for (int r = 0; r < 4; r++) {
                    if (!viewModel.otvorenaPolja[k][r]) {
                        viewModel.otvorenaPolja[k][r] = true;
                        otkriPolje(k, r);
                    }
                }
                if (!viewModel.pogodenaKolona[k]) {
                    viewModel.pogodenaKolona[k] = true;
                    oznaciPogodenuKolonu(k);
                }
            }
            binding.btnKonacno.setText(AsocijacijeViewModel.KONACNO_RJESENJE);
            binding.btnKonacno.setBackgroundTintList(ColorStateList.valueOf(BOJA_OTKRIVENO));
            binding.btnKonacno.setTextColor(Color.WHITE);
            Toast.makeText(getContext(), "Tačno! +" + bodovi + " bod.", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.pogodenaKolona[kolona] = true;
            int bodovi = izracunajBodoveKolone(kolona); // score se računa prije otkrivanja
            dodajBodove(bodovi);
            // Otkrij sva preostala polja te kolone
            for (int r = 0; r < 4; r++) {
                if (!viewModel.otvorenaPolja[kolona][r]) {
                    viewModel.otvorenaPolja[kolona][r] = true;
                    otkriPolje(kolona, r);
                }
            }
            oznaciPogodenuKolonu(kolona);
            Toast.makeText(getContext(), "Tačno! +" + bodovi + " bod.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dodajBodove(int bodovi) {
        Integer trenutno = viewModel.score.getValue();
        viewModel.score.setValue((trenutno == null ? 0 : trenutno) + bodovi);
    }

    private int izracunajBodoveKolone(int k) {
        int neotvorena = 0;
        for (int r = 0; r < 4; r++) {
            if (!viewModel.otvorenaPolja[k][r]) neotvorena++;
        }
        return 2 + neotvorena;
    }

    private int izracunajBodoveKonacno() {
        int bodovi = 7;
        for (int k = 0; k < 4; k++) {
            boolean imaOtvorenih = false;
            for (int r = 0; r < 4; r++) {
                if (viewModel.otvorenaPolja[k][r]) { imaOtvorenih = true; break; }
            }
            if (!imaOtvorenih && !viewModel.pogodenaKolona[k]) {
                bodovi += 6;
            } else {
                bodovi += izracunajBodoveKolone(k);
            }
        }
        return bodovi;
    }

    private void onemogucuiInterakciju() {
        for (int k = 0; k < 4; k++) {
            for (int r = 0; r < 4; r++) {
                itemDugmad[k][r].setEnabled(false);
            }
            if (!viewModel.pogodenaKolona[k]) rezDugmad[k].setEnabled(false);
        }
        if (!viewModel.konacnoPogodeno) binding.btnKonacno.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        binding = null;
    }
}
