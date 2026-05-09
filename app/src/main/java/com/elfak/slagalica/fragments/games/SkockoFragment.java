package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentSkockoBinding;

public class SkockoFragment extends Fragment {

    private static final long TRAJANJE_IGRAC = 30_000;
    private static final long TRAJANJE_PROTIVNIK = 10_000;

    private FragmentSkockoBinding binding;
    private SkockoViewModel viewModel;
    private CountDownTimer tajmer;

    private Button[][] slotButtons;
    private TextView[] feedbackViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSkockoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);
        viewModel.score.observe(getViewLifecycleOwner(),
                s -> binding.tvBodovi.setText("Bodovi: " + s));

        viewModel.inicijalizujTajnu();
        inicijalizujSlotove();
        obnavljanjeUI();
        obnavljanjeTajmera();

        binding.btnSimbol1.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[0]));
        binding.btnSimbol2.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[1]));
        binding.btnSimbol3.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[2]));
        binding.btnSimbol4.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[3]));
        binding.btnSimbol5.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[4]));
        binding.btnSimbol6.setOnClickListener(v -> dodajSimbol(SkockoViewModel.SIMBOLI[5]));

        binding.btnPotvrdaPokusaja.setOnClickListener(v -> potvrdaPokusaja());
    }

    private void inicijalizujSlotove() {
        slotButtons = new Button[][]{
            {binding.btnSlotR1P1, binding.btnSlotR1P2, binding.btnSlotR1P3, binding.btnSlotR1P4},
            {binding.btnSlotR2P1, binding.btnSlotR2P2, binding.btnSlotR2P3, binding.btnSlotR2P4},
            {binding.btnSlotR3P1, binding.btnSlotR3P2, binding.btnSlotR3P3, binding.btnSlotR3P4},
            {binding.btnSlotR4P1, binding.btnSlotR4P2, binding.btnSlotR4P3, binding.btnSlotR4P4},
            {binding.btnSlotR5P1, binding.btnSlotR5P2, binding.btnSlotR5P3, binding.btnSlotR5P4},
            {binding.btnSlotR6P1, binding.btnSlotR6P2, binding.btnSlotR6P3, binding.btnSlotR6P4},
        };
        feedbackViews = new TextView[]{
            binding.tvFeedbackR1, binding.tvFeedbackR2, binding.tvFeedbackR3,
            binding.tvFeedbackR4, binding.tvFeedbackR5, binding.tvFeedbackR6
        };

        for (int r = 0; r < 6; r++) {
            final int red = r;
            for (int p = 0; p < 4; p++) {
                final int pozicija = p;
                slotButtons[r][p].setOnClickListener(v -> klikNaSlot(red, pozicija));
            }
        }
    }

    // Poziva se i pri prvom startu i pri rotaciji — obnavlja UI iz ViewModel stanja
    private void obnavljanjeUI() {
        for (int r = 0; r < 6; r++) {
            for (int p = 0; p < 4; p++) {
                String s = viewModel.tabla[r][p];
                slotButtons[r][p].setText(s != null ? s : "");
            }
            String fb = viewModel.feedbackovi[r];
            feedbackViews[r].setText(fb != null ? fb : "");
        }

        if (viewModel.rjesenje != null) {
            binding.btnSlotR8P1.setText(viewModel.rjesenje[0]);
            binding.btnSlotR8P2.setText(viewModel.rjesenje[1]);
            binding.btnSlotR8P3.setText(viewModel.rjesenje[2]);
            binding.btnSlotR8P4.setText(viewModel.rjesenje[3]);
        }

        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) {
            setSimboliEnabled(false);
            binding.btnPotvrdaPokusaja.setEnabled(false);
        }
    }

    // Restartuje tajmer s preostalim vremenom — ključno za preživljavanje rotacije
    private void obnavljanjeTajmera() {
        long sada = System.currentTimeMillis();

        switch (viewModel.faza) {
            case IGRAC:
                if (viewModel.timerEndMs == 0) {
                    viewModel.timerEndMs = sada + TRAJANJE_IGRAC;
                }
                long preostaloIgrac = viewModel.timerEndMs - sada;
                if (preostaloIgrac > 0) {
                    startTajmerIgrac(preostaloIgrac);
                } else {
                    onTajmerIgracaIstekao();
                }
                break;

            case PROTIVNIK:
                long preostaloProtivnik = viewModel.timerEndMs - sada;
                if (preostaloProtivnik > 0) {
                    startTajmerProtivnik(preostaloProtivnik);
                } else {
                    viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
                    prikaziRjesenje();
                    binding.tvTajmer.setText("Završeno");
                }
                break;

            case ZAVRSENO:
                binding.tvTajmer.setText("Završeno");
                break;
        }
    }

    private void klikNaSlot(int red, int pozicija) {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        if (red != viewModel.aktivniRed) return;
        if (viewModel.aktivniSlot == 0) return;
        if (pozicija != viewModel.aktivniSlot - 1) return;

        viewModel.tabla[red][pozicija] = null;
        slotButtons[red][pozicija].setText("");
        viewModel.aktivniSlot--;
    }

    private void dodajSimbol(String simbol) {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        if (viewModel.aktivniRed >= 6) return;
        if (viewModel.aktivniSlot >= 4) return;

        viewModel.tabla[viewModel.aktivniRed][viewModel.aktivniSlot] = simbol;
        slotButtons[viewModel.aktivniRed][viewModel.aktivniSlot].setText(simbol);
        viewModel.aktivniSlot++;
    }

    private void potvrdaPokusaja() {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;

        if (viewModel.aktivniSlot < 4) {
            Toast.makeText(getContext(), "Morate izabrati 4 simbola!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] pokusaj = viewModel.tabla[viewModel.aktivniRed].clone();
        String feedback = izracunajFeedback(pokusaj, viewModel.tajnaKombinacija);
        viewModel.feedbackovi[viewModel.aktivniRed] = feedback;
        feedbackViews[viewModel.aktivniRed].setText(feedback);

        if (feedback.equals("●●●●")) {
            int bodovi = izracunajBodove();
            viewModel.score.setValue(bodovi);
            viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
            if (tajmer != null) tajmer.cancel();
            Toast.makeText(getContext(),
                    "Pogodili ste! Bodovi: " + bodovi, Toast.LENGTH_LONG).show();
            prikaziRjesenje();
            return;
        }

        viewModel.aktivniRed++;
        viewModel.aktivniSlot = 0;

        if (viewModel.aktivniRed >= 6) {
            viewModel.faza = SkockoViewModel.Faza.PROTIVNIK;
            if (tajmer != null) tajmer.cancel();
            Toast.makeText(getContext(),
                    "Niste pogodili! Protivnik dobija šansu.", Toast.LENGTH_SHORT).show();
            pokreniTajmerProtivnika();
        }
    }

    private String izracunajFeedback(String[] pokusaj, String[] tajna) {
        boolean[] oznacenPokusaj = new boolean[4];
        boolean[] oznacenaTajna = new boolean[4];
        int tacno = 0, djelimicno = 0;

        for (int i = 0; i < 4; i++) {
            if (pokusaj[i].equals(tajna[i])) {
                tacno++;
                oznacenPokusaj[i] = true;
                oznacenaTajna[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (oznacenPokusaj[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (!oznacenaTajna[j] && pokusaj[i].equals(tajna[j])) {
                    djelimicno++;
                    oznacenaTajna[j] = true;
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tacno; i++) sb.append("●");
        for (int i = 0; i < djelimicno; i++) sb.append("○");
        return sb.toString();
    }

    private int izracunajBodove() {
        if (viewModel.aktivniRed <= 1) return 20;
        if (viewModel.aktivniRed <= 3) return 15;
        return 10;
    }

    private void startTajmerIgrac(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("⏱ " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                onTajmerIgracaIstekao();
            }
        }.start();
    }

    private void onTajmerIgracaIstekao() {
        if (viewModel.faza != SkockoViewModel.Faza.IGRAC) return;
        binding.tvTajmer.setText("⏱ 0s");
        viewModel.faza = SkockoViewModel.Faza.PROTIVNIK;
        Toast.makeText(getContext(),
                "Isteklo vreme! Protivnik dobija šansu.", Toast.LENGTH_SHORT).show();
        pokreniTajmerProtivnika();
    }

    private void pokreniTajmerProtivnika() {
        setSimboliEnabled(false);
        binding.btnPotvrdaPokusaja.setEnabled(false);
        viewModel.timerEndMs = System.currentTimeMillis() + TRAJANJE_PROTIVNIK;
        startTajmerProtivnik(TRAJANJE_PROTIVNIK);
    }

    private void startTajmerProtivnik(long trajanje) {
        tajmer = new CountDownTimer(trajanje, 1000) {
            @Override public void onTick(long ms) {
                binding.tvTajmer.setText("Protivnik: " + ms / 1000 + "s");
            }
            @Override public void onFinish() {
                binding.tvTajmer.setText("Završeno");
                viewModel.faza = SkockoViewModel.Faza.ZAVRSENO;
                prikaziRjesenje();
            }
        }.start();
    }

    private void setSimboliEnabled(boolean enabled) {
        binding.btnSimbol1.setEnabled(enabled);
        binding.btnSimbol2.setEnabled(enabled);
        binding.btnSimbol3.setEnabled(enabled);
        binding.btnSimbol4.setEnabled(enabled);
        binding.btnSimbol5.setEnabled(enabled);
        binding.btnSimbol6.setEnabled(enabled);
    }

    private void prikaziRjesenje() {
        viewModel.rjesenje = viewModel.tajnaKombinacija;
        binding.btnSlotR8P1.setText(viewModel.tajnaKombinacija[0]);
        binding.btnSlotR8P2.setText(viewModel.tajnaKombinacija[1]);
        binding.btnSlotR8P3.setText(viewModel.tajnaKombinacija[2]);
        binding.btnSlotR8P4.setText(viewModel.tajnaKombinacija[3]);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        binding = null;
    }
}
