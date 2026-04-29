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

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

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

        // Klik na dugme Prijavi se
        binding.btnPrijava.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String lozinka = binding.etLozinka.getText().toString().trim();

            if (email.isEmpty() || lozinka.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite email i lozinku!", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Firebase login logika dolazi u KT2
            Toast.makeText(getContext(), "Prijava...", Toast.LENGTH_SHORT).show();
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