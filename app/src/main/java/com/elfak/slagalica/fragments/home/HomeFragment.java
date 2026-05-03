package com.elfak.slagalica.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentHomeBinding;
import com.elfak.slagalica.repository.AuthRepository;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        // Prikaži email ulogovanog korisnika
        if (authRepository.trenutniKorisnik() != null) {
            binding.tvKorisnik.setText(authRepository.trenutniKorisnik().getEmail());
        }

        // Klik na Odjavi se
        binding.btnOdjava.setOnClickListener(v -> {
            authRepository.odjava();
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_loginFragment);
        });
        // Klik na Promijeni lozinku
        binding.btnPromjeniLozinku.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_resetLozinkeFragment);
        });

        binding.btnIgraj.setOnClickListener(v -> {
            // TODO: Provjeri tokene iz Firestore

            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_cekanjeFragment);
        });

        binding.btnKorakPoKorak.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_korakPoKorakFragment);
        });

        binding.btnMojBroj.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_mojBrojFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}