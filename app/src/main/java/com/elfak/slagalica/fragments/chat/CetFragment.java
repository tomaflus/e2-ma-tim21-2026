package com.elfak.slagalica.fragments.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.adapters.PorukaAdapter;
import com.elfak.slagalica.databinding.FragmentCetBinding;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.CetRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class CetFragment extends Fragment {

    private FragmentCetBinding binding;
    private CetRepository cetRepository;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private PorukaAdapter adapter;

    private String mojId;
    private String mojIme;
    private String mojRegion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cetRepository = new CetRepository();
        authRepository = new AuthRepository();
        userRepository = new UserRepository();

        mojId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Postavi RecyclerView
        adapter = new PorukaAdapter(new ArrayList<>(), mojId);
        binding.rvPoruke.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPoruke.setAdapter(adapter);

        // Dohvati podatke korisnika i ucitaj poruke
        userRepository.dohvatiKorisnika(
                user -> {
                    mojIme = user.getKorisnickoIme() != null ?
                            user.getKorisnickoIme() : user.getEmail();
                    mojRegion = user.getRegion() != null ?
                            user.getRegion() : "Opsti";

                    // Prikaži naziv regiona
                    binding.tvNaslovCeta.setText("Chat - " + mojRegion);

                    // Ucitaj poruke za region
                    ucitajPoruke();
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );

        // Klik na Pošalji
        binding.btnPosalji.setOnClickListener(v -> {
            String tekst = binding.etPoruka.getText().toString().trim();
            if (tekst.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite poruku!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mojRegion == null) {
                Toast.makeText(getContext(),
                        "Region nije ucitan!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnPosalji.setEnabled(false);

            cetRepository.pošaljiPoruku(tekst, mojIme, mojRegion,
                    () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.etPoruka.setText("");
                                binding.btnPosalji.setEnabled(true);
                            });
                        }
                    },
                    poruka -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnPosalji.setEnabled(true);
                                Toast.makeText(getContext(),
                                        "Greska: " + poruka,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
    }

    private void ucitajPoruke() {
        cetRepository.slušajPoruke(mojRegion,
                poruke -> {
                    adapter.azurirajPoruke(poruke);
                    // Skroluj na zadnju poruku
                    if (!poruke.isEmpty()) {
                        binding.rvPoruke.scrollToPosition(poruke.size() - 1);
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cetRepository.ukloniListener();
        binding = null;
    }
}