package com.elfak.slagalica.fragments.game;

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
import com.elfak.slagalica.databinding.FragmentCekanjeBinding;
import com.elfak.slagalica.model.Partija;
import com.elfak.slagalica.model.StatusPartije;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.PartijaRepository;

public class CekanjeFragment extends Fragment {

    private FragmentCekanjeBinding binding;
    private PartijaRepository partijaRepository;
    private AuthRepository authRepository;
    private String partijaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCekanjeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        partijaRepository = new PartijaRepository();
        authRepository = new AuthRepository();

        // Dohvati korisničko ime
        String korisnickoIme = authRepository.trenutniKorisnik() != null
                ? authRepository.trenutniKorisnik().getEmail()
                : "Nepoznat";

        // Traži partiju
        traziPartiju(korisnickoIme);

        // Odustani
        binding.btnOdustani.setOnClickListener(v -> {
            if (partijaId != null) {
                partijaRepository.napustiPartiju(partijaId,
                        () -> Navigation.findNavController(view).popBackStack(),
                        poruka -> Toast.makeText(getContext(),
                                poruka, Toast.LENGTH_SHORT).show());
            } else {
                Navigation.findNavController(view).popBackStack();
            }
            partijaRepository.ukloniListener();
        });
    }

    private void traziPartiju(String korisnickoIme) {
        binding.tvStatus.setText("Trazenje protivnika...");

        partijaRepository.kreirajPartiju(korisnickoIme,
                partija -> {
                    partijaId = partija.getId();

                    if (partija.getStatus() == StatusPartije.U_TOKU) {
                        // Pronasli smo protivnika — idemo na igru
                        binding.tvStatus.setText("Protivnik pronadjen! Pokretanje igre...");

                        Bundle args = new Bundle();
                        args.putString("partijaId", partija.getId());
                        args.putBoolean("jeIgrac1",
                                partija.getIgrac1Id().equals(
                                        authRepository.trenutniKorisnik().getUid()));

                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_cekanjeFragment_to_igraFragment, args);

                        partijaRepository.ukloniListener();
                    } else {
                        binding.tvStatus.setText("Cekanje na protivnika...");
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        partijaRepository.ukloniListener();
        binding = null;
    }
}