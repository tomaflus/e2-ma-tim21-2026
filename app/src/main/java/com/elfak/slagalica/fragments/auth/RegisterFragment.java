package com.elfak.slagalica.fragments.auth;

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
import com.elfak.slagalica.databinding.FragmentRegisterBinding;
import com.elfak.slagalica.repository.AuthRepository;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        // Klik na dugme Registruj se
        binding.btnRegistracija.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String korisnickoIme = binding.etKorisnickoIme.getText().toString().trim();
            String region = binding.etRegion.getText().toString().trim();
            String lozinka = binding.etLozinka.getText().toString().trim();
            String ponovljenaLozinka = binding.etPonovljenaLozinka.getText().toString().trim();

            // Validacija polja
            if (email.isEmpty() || korisnickoIme.isEmpty() ||
                    region.isEmpty() || lozinka.isEmpty() || ponovljenaLozinka.isEmpty()) {
                Toast.makeText(getContext(),
                        "Sva polja su obavezna!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!lozinka.equals(ponovljenaLozinka)) {
                Toast.makeText(getContext(),
                        "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lozinka.length() < 6) {
                Toast.makeText(getContext(),
                        "Lozinka mora imati najmanje 6 karaktera!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Onemogući dugme tokom registracije
            binding.btnRegistracija.setEnabled(false);

            authRepository.registracija(email, lozinka, korisnickoIme, region,
                    () -> {
                        // Uspješna registracija
                        binding.btnRegistracija.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Registracija uspješna! Provjerite email za verifikaciju.",
                                Toast.LENGTH_LONG).show();

                        // Vrati na Login ekran
                        Navigation.findNavController(view)
                                .navigate(R.id.action_registerFragment_to_loginFragment);
                    },
                    poruka -> {
                        // Greška
                        binding.btnRegistracija.setEnabled(true);
                        Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
                    });
        });

        // Klik na "Već imate nalog? Prijavite se"
        binding.tvPrijava.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}