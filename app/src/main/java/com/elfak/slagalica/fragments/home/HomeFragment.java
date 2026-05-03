package com.elfak.slagalica.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentHomeBinding;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.UserRepository;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;

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
        userRepository = new UserRepository();

        // Prikaži email korisnika
        if (authRepository.trenutniKorisnik() != null) {
            binding.tvKorisnik.setText(authRepository.trenutniKorisnik().getEmail());
        }

        // Dodaj dnevne tokene pri svakom otvaranju Home ekrana
        userRepository.dodajDnevneTokene(
                () -> ucitajTokene(),
                poruka -> ucitajTokene()
        );

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

        // Klik na Igraj — provjeri tokene
        binding.btnIgraj.setOnClickListener(v -> {
            userRepository.oduzmiToken(
                    () -> {
                        // Token oduzet — idi na čekanje
                        ucitajTokene(); // osvježi prikaz
                        Navigation.findNavController(view)
                                .navigate(R.id.action_homeFragment_to_cekanjeFragment);
                    },
                    poruka -> Toast.makeText(getContext(),
                            poruka, Toast.LENGTH_LONG).show()
            );
        });

        // Test dugmad
        binding.btnKorakPoKorak.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_korakPoKorakFragment);
        });

        binding.btnMojBroj.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_mojBrojFragment);
        });
    }

    private void ucitajTokene() {
        userRepository.dohvatiKorisnika(
                user -> binding.tvKorisnik.setText(
                        user.getEmail() + " | Tokeni: " + user.getTokeni() +
                                " | Zvezde: " + user.getZvezde()),
                poruka -> {}
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}