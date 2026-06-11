package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentAsocijacijeBinding;
import com.elfak.slagalica.repository.AsocijacijeRepository;
import com.elfak.slagalica.service.AsocijacijeService;
import com.elfak.slagalica.service.stats.StatsService;

public class AsocijacijeFragment extends Fragment {
    private FragmentAsocijacijeBinding binding;
    private AsocijacijeService service;
    private CountDownTimer timer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAsocijacijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        service = new AsocijacijeService(new AsocijacijeRepository(), new StatsService(requireContext()));

        setupFieldClicks();
        
        binding.btnSubmit.setOnClickListener(v -> {
            checkGuesses();
        });

        startTimer();
        render();
    }

    private void setupFieldClicks() {
        binding.btnA1.setOnClickListener(v -> openField("A1", (Button) v));
        binding.btnA2.setOnClickListener(v -> openField("A2", (Button) v));
        binding.btnA3.setOnClickListener(v -> openField("A3", (Button) v));
        binding.btnA4.setOnClickListener(v -> openField("A4", (Button) v));

        binding.btnB1.setOnClickListener(v -> openField("B1", (Button) v));
        binding.btnB2.setOnClickListener(v -> openField("B2", (Button) v));
        binding.btnB3.setOnClickListener(v -> openField("B3", (Button) v));
        binding.btnB4.setOnClickListener(v -> openField("B4", (Button) v));

        binding.btnC1.setOnClickListener(v -> openField("C1", (Button) v));
        binding.btnC2.setOnClickListener(v -> openField("C2", (Button) v));
        binding.btnC3.setOnClickListener(v -> openField("C3", (Button) v));
        binding.btnC4.setOnClickListener(v -> openField("C4", (Button) v));

        binding.btnD1.setOnClickListener(v -> openField("D1", (Button) v));
        binding.btnD2.setOnClickListener(v -> openField("D2", (Button) v));
        binding.btnD3.setOnClickListener(v -> openField("D3", (Button) v));
        binding.btnD4.setOnClickListener(v -> openField("D4", (Button) v));
    }

    private void openField(String id, Button b) {
        if (!service.getOtvorenaPolja().contains(id)) {
            service.otvoriPolje(id);
            render();
            // Po pravilima d: "Nakon svakog otvorenog polja, igrač može da pogađa"
            // U single playeru samo prebacujemo focus na inpute
        }
    }

    private void checkGuesses() {
        boolean guessedAny = false;
        
        String guessA = binding.etA.getText().toString();
        if (!guessA.isEmpty() && !service.isReseno('A')) {
            if (service.pogodiKolonu('A', guessA)) guessedAny = true;
        }

        String guessKonacno = binding.etKonacno.getText().toString();
        if (!guessKonacno.isEmpty()) {
            if (service.pogodiKonacno(guessKonacno)) {
                guessedAny = true;
                finishRound();
                return;
            }
        }
        
        if (!guessedAny) {
            Toast.makeText(getContext(), "Netačno!", Toast.LENGTH_SHORT).show();
            // service.zameniIgraca(); // Za multiplayer
        }
        render();
    }

    private void render() {
        if (binding == null) return;
        AsocijacijeRepository.Asocijacija data = service.getCurrent();
        
        setBtn(binding.btnA1, "A1", data.a1);
        setBtn(binding.btnA2, "A2", data.a2);
        setBtn(binding.btnA3, "A3", data.a3);
        setBtn(binding.btnA4, "A4", data.a4);
        if (service.isReseno('A')) {
            binding.etA.setText(data.resA);
            binding.etA.setEnabled(false);
            binding.etA.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
        }

        setBtn(binding.btnB1, "B1", data.b1);
        setBtn(binding.btnB2, "B2", data.b2);
        setBtn(binding.btnB3, "B3", data.b3);
        setBtn(binding.btnB4, "B4", data.b4);
        if (service.isReseno('B')) {
            binding.etB.setText(data.resB);
            binding.etB.setEnabled(false);
            binding.etB.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
        }

        setBtn(binding.btnC1, "C1", data.c1);
        setBtn(binding.btnC2, "C2", data.c2);
        setBtn(binding.btnC3, "C3", data.c3);
        setBtn(binding.btnC4, "C4", data.c4);
        if (service.isReseno('C')) {
            binding.etC.setText(data.resC);
            binding.etC.setEnabled(false);
            binding.etC.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
        }

        setBtn(binding.btnD1, "D1", data.d1);
        setBtn(binding.btnD2, "D2", data.d2);
        setBtn(binding.btnD3, "D3", data.d3);
        setBtn(binding.btnD4, "D4", data.d4);
        if (service.isReseno('D')) {
            binding.etD.setText(data.resD);
            binding.etD.setEnabled(false);
            binding.etD.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
        }
        
        if (service.isResenoKonacno()) {
            binding.etKonacno.setText(data.konacno);
            binding.etKonacno.setEnabled(false);
            binding.etKonacno.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
        }

        binding.tvScore.setText("Ja: " + service.getScore());
        binding.tvRound.setText("Runda: " + service.getRound() + "/2");
    }

    private void setBtn(Button b, String id, String text) {
        if (service.getOtvorenaPolja().contains(id)) {
            b.setText(text);
            b.setBackgroundColor(getResources().getColor(R.color.dividerBoja, null));
        } else {
            b.setText(id);
        }
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(120000, 1000) {
            public void onTick(long m) { 
                if (binding != null) binding.tvTimer.setText("Vreme: " + (m/1000) + "s"); 
            }
            public void onFinish() { finishRound(); }
        }.start();
    }

    private void finishRound() {
        if (service.getRound() < 2) {
            service.loadRound(2);
            render();
            startTimer();
        } else {
            // Kraj igre
            if (timer != null) timer.cancel();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
        binding = null;
    }
}