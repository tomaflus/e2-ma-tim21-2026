package com.elfak.slagalica.fragments.izazov;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.adapters.IzazovAdapter;
import com.elfak.slagalica.databinding.FragmentIzazovBinding;
import com.elfak.slagalica.model.Izazov;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.IzazovRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class IzazovFragment extends Fragment {

    private FragmentIzazovBinding binding;
    private IzazovRepository izazovRepository;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private IzazovAdapter adapter;

    private String mojId;
    private String mojIme;
    private String mojRegion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentIzazovBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        izazovRepository = new IzazovRepository();
        authRepository = new AuthRepository();
        userRepository = new UserRepository();

        mojId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Postavi RecyclerView
        adapter = new IzazovAdapter(new ArrayList<>(), mojId, this::prihvatiIzazov);
        binding.rvIzazovi.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvIzazovi.setAdapter(adapter);

        // Dohvati podatke korisnika
        userRepository.dohvatiKorisnika(
                user -> {
                    mojIme = user.getKorisnickoIme() != null ?
                            user.getKorisnickoIme() : user.getEmail();
                    mojRegion = user.getRegion() != null ?
                            user.getRegion() : "Opsti";

                    // Ucitaj izazove za region
                    ucitajIzazove();
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );

        // Klik na Postavi izazov
        binding.btnKreirajIzazov.setOnClickListener(v -> {
            String zvezdeStr = binding.etZvezdeUlog.getText().toString().trim();
            String tokeniStr = binding.etTokeniUlog.getText().toString().trim();

            if (zvezdeStr.isEmpty() || tokeniStr.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite ulog!", Toast.LENGTH_SHORT).show();
                return;
            }

            int zvezde = Integer.parseInt(zvezdeStr);
            int tokeni = Integer.parseInt(tokeniStr);

            // Validacija
            if (zvezde < 0 || zvezde > 10) {
                Toast.makeText(getContext(),
                        "Zvezde moraju biti između 0 i 10!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (tokeni < 0 || tokeni > 2) {
                Toast.makeText(getContext(),
                        "Tokeni moraju biti između 0 i 2!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (mojRegion == null) {
                Toast.makeText(getContext(),
                        "Region nije ucitan!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnKreirajIzazov.setEnabled(false);

            izazovRepository.kreirajIzazov(mojIme, mojRegion, zvezde, tokeni,
                    izazovId -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnKreirajIzazov.setEnabled(true);
                                binding.etZvezdeUlog.setText("");
                                binding.etTokeniUlog.setText("");
                                Toast.makeText(getContext(),
                                        "Izazov postavljen!",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    },
                    poruka -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnKreirajIzazov.setEnabled(true);
                                Toast.makeText(getContext(),
                                        "Greska: " + poruka,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
    }

    private void ucitajIzazove() {
        izazovRepository.slušajIzazove(mojRegion,
                izazovi -> adapter.azurirajIzazove(izazovi),
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );
    }

    private void prihvatiIzazov(Izazov izazov) {
        // Provjeri da li ima dovoljno zvezda i tokena
        userRepository.dohvatiKorisnika(
                user -> {
                    if (user.getZvezde() < izazov.getZvezdeUlog()) {
                        Toast.makeText(getContext(),
                                "Nemate dovoljno zvezda!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (user.getTokeni() < izazov.getTokeniUlog()) {
                        Toast.makeText(getContext(),
                                "Nemate dovoljno tokena!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Prihvati izazov
                    izazovRepository.prihvatiIzazov(izazov.getId(), mojIme,
                            izazovId -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(),
                                                    "Izazov prihvacen!",
                                                    Toast.LENGTH_SHORT).show());
                                }
                            },
                            poruka -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(),
                                                    "Greska: " + poruka,
                                                    Toast.LENGTH_SHORT).show());
                                }
                            });
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        izazovRepository.ukloniListener();
        binding = null;
    }
}