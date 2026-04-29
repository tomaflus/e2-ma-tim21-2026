package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentKorakPoKorakBinding;

public class KorakPoKorakFragment extends Fragment {

    private FragmentKorakPoKorakBinding binding;

    // Primjer koraka (u KT2 dolaze iz Firebase baze)
    private String[] koraci = {
            "Korak 1: Nalazi se u Africi",
            "Korak 2: Ima crno-bijele pruge",
            "Korak 3: Živi u stepama",
            "Korak 4: Biljojed je",
            "Korak 5: Veoma je brz",
            "Korak 6: Srodan je konju",
            "Korak 7: Ima grivu"
    };

    private int trenutniKorak = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKorakPoKorakBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Prikaži prvi korak
        prikaziKorak(0);

        // Klik na dugme Odgovori
        binding.btnOdgovori.setOnClickListener(v -> {
            String odgovor = binding.etOdgovor.getText().toString().trim();

            if (odgovor.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite odgovor!", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Provjera odgovora dolazi u KT2
            Toast.makeText(getContext(),
                    "Odgovor: " + odgovor, Toast.LENGTH_SHORT).show();

            // Prikaži sljedeći korak
            if (trenutniKorak < koraci.length - 1) {
                trenutniKorak++;
                prikaziKorak(trenutniKorak);
                binding.etOdgovor.setText("");
            }
        });
    }

    private void prikaziKorak(int index) {
        // Sakrij sve korake
        binding.tvKorak1.setVisibility(View.GONE);
        binding.tvKorak2.setVisibility(View.GONE);
        binding.tvKorak3.setVisibility(View.GONE);
        binding.tvKorak4.setVisibility(View.GONE);
        binding.tvKorak5.setVisibility(View.GONE);
        binding.tvKorak6.setVisibility(View.GONE);
        binding.tvKorak7.setVisibility(View.GONE);

        // Prikaži korake do trenutnog
        if (index >= 0) {
            binding.tvKorak1.setText(koraci[0]);
            binding.tvKorak1.setVisibility(View.VISIBLE);
        }
        if (index >= 1) {
            binding.tvKorak2.setText(koraci[1]);
            binding.tvKorak2.setVisibility(View.VISIBLE);
        }
        if (index >= 2) {
            binding.tvKorak3.setText(koraci[2]);
            binding.tvKorak3.setVisibility(View.VISIBLE);
        }
        if (index >= 3) {
            binding.tvKorak4.setText(koraci[3]);
            binding.tvKorak4.setVisibility(View.VISIBLE);
        }
        if (index >= 4) {
            binding.tvKorak5.setText(koraci[4]);
            binding.tvKorak5.setVisibility(View.VISIBLE);
        }
        if (index >= 5) {
            binding.tvKorak6.setText(koraci[5]);
            binding.tvKorak6.setVisibility(View.VISIBLE);
        }
        if (index >= 6) {
            binding.tvKorak7.setText(koraci[6]);
            binding.tvKorak7.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}