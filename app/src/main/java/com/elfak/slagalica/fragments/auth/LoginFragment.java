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
import com.elfak.slagalica.databinding.FragmentLoginBinding;
import com.elfak.slagalica.repository.AuthRepository;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        if (authRepository.jeUlogovan()) {
            // TODO: navigacija na glavni ekran dolazi u KO
            Toast.makeText(getContext(), "Već ste ulogovani!", Toast.LENGTH_SHORT).show();
        }

        // Klik na dugme Prijavi se
        binding.btnPrijava.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String lozinka = binding.etLozinka.getText().toString().trim();

            if (email.isEmpty() || lozinka.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite email i lozinku!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Onemogući dugme tokom logovanja
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnPrijava.setEnabled(false);

            authRepository.logovanje(email, lozinka,
                    () -> {
                        // Uspješno logovanje
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnPrijava.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Uspješno ste se prijavili!", Toast.LENGTH_SHORT).show();
                        // TODO: navigacija na glavni ekran dolazi u KO
                    },
                    poruka -> {
                        // Greška
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnPrijava.setEnabled(true);
                        Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
                    });
            });

        // Klik na "Zaboravili ste lozinku?"
        binding.tvZaboravljenaLozinka.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite email za reset lozinke!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);

            authRepository.resetLozinke(email,
                    () -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Email za reset lozinke je poslan!",
                                Toast.LENGTH_LONG).show();
                    },
                    poruka -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
                    });
        });

        // Klik na "Nemate nalog? Registrujte se"
        binding.tvRegistracija.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}