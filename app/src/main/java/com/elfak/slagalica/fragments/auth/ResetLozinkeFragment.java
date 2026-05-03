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
import com.elfak.slagalica.databinding.FragmentResetLozinkeBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetLozinkeFragment extends Fragment {

    private FragmentResetLozinkeBinding binding;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResetLozinkeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        binding.btnPromjeniLozinku.setOnClickListener(v -> {
            String staraLozinka = binding.etStaraLozinka.getText().toString().trim();
            String novaLozinka = binding.etNovaLozinka.getText().toString().trim();
            String ponovljenaNovaLozinka = binding.etPonovljenaNovaLozinka.getText().toString().trim();

            // Validacija
            if (staraLozinka.isEmpty() || novaLozinka.isEmpty() || ponovljenaNovaLozinka.isEmpty()) {
                Toast.makeText(getContext(),
                        "Sva polja su obavezna!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!novaLozinka.equals(ponovljenaNovaLozinka)) {
                Toast.makeText(getContext(),
                        "Nove lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (novaLozinka.length() < 6) {
                Toast.makeText(getContext(),
                        "Nova lozinka mora imati najmanje 6 karaktera!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser korisnik = auth.getCurrentUser();
            if (korisnik == null || korisnik.getEmail() == null) {
                Toast.makeText(getContext(),
                        "Niste ulogovani!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnPromjeniLozinku.setEnabled(false);

            // Ponovna autentifikacija sa starom lozinkom
            AuthCredential credential = EmailAuthProvider
                    .getCredential(korisnik.getEmail(), staraLozinka);

            korisnik.reauthenticate(credential)
                    .addOnSuccessListener(unused -> {
                        // Promijeni lozinku
                        korisnik.updatePassword(novaLozinka)
                                .addOnSuccessListener(unused2 -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnPromjeniLozinku.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Lozinka uspjesno promijenjena!",
                                            Toast.LENGTH_LONG).show();
                                    Navigation.findNavController(view).popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnPromjeniLozinku.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnPromjeniLozinku.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Stara lozinka nije ispravna!", Toast.LENGTH_LONG).show();
                    });
        });

        // Nazad
        binding.tvNazad.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}