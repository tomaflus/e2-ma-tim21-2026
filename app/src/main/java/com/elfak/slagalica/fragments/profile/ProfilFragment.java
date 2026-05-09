package com.elfak.slagalica.fragments.profile;

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
import com.elfak.slagalica.databinding.FragmentProfilBinding;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class ProfilFragment extends Fragment {

    private FragmentProfilBinding binding;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();
        FirebaseUser firebaseUser = authRepository.trenutniKorisnik();

        if (firebaseUser != null) {
            authRepository.getKorisnikPodaci(firebaseUser.getUid(), (User user) -> {
                if (user != null && binding != null) {
                    binding.tvKorisnickoIme.setText(user.getKorisnickoIme());
                    binding.tvEmail.setText(user.getEmail());
                    binding.tvTokeni.setText(String.valueOf(user.getTokeni()));
                    binding.tvZvezde.setText(String.valueOf(user.getZvezde()));
                    binding.tvRegion.setText(user.getRegion());

                    String ligaStr;
                    if (user.getLiga() == 1) ligaStr = "Srebrna";
                    else if (user.getLiga() == 2) ligaStr = "Zlatna";
                    else ligaStr = "Bronzana";
                    binding.tvLiga.setText(ligaStr);
                }
            });
        }

        binding.btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Promena avatara (KT2)", Toast.LENGTH_SHORT).show();
        });

        binding.btnLogout.setOnClickListener(v -> {
            authRepository.odjava();
            Navigation.findNavController(view).navigate(R.id.action_profilFragment_to_loginFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}