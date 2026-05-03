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
import com.elfak.slagalica.databinding.FragmentIgraBinding;
import com.elfak.slagalica.model.Partija;
import com.elfak.slagalica.model.StatusPartije;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.PartijaRepository;

public class IgraFragment extends Fragment {

    private FragmentIgraBinding binding;
    private PartijaRepository partijaRepository;
    private AuthRepository authRepository;

    private String partijaId;
    private boolean jeIgrac1;
    private Partija trenutnaPartija;

    // Redoslijed igara
    private static final String[] IGRE = {
            "Ko zna zna",
            "Spojnice",
            "Asocijacije",
            "Skocko",
            "Korak po korak",
            "Moj broj"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentIgraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        partijaRepository = new PartijaRepository();
        authRepository = new AuthRepository();

        // Dohvati argumente
        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
        }

        // Slušaj promjene partije
        partijaRepository.slušajPartiju(partijaId,
                partija -> {
                    trenutnaPartija = partija;
                    azurirajUI(partija);

                    if (partija.getStatus() == StatusPartije.ZAVRSENA) {
                        prikaziRezultat(partija);
                    } else if (partija.getStatus() == StatusPartije.NAPUSTENA) {
                        Toast.makeText(getContext(),
                                "Protivnik je napustio partiju!",
                                Toast.LENGTH_LONG).show();
                        Navigation.findNavController(view)
                                .navigate(R.id.action_igraFragment_to_homeFragment);
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show());

        // Pokreni prvu igru
        pokrniIgru(0);
    }

    private void azurirajUI(Partija partija) {
        binding.tvIgrac1.setText(
                (partija.getIgrac1Ime() != null ? partija.getIgrac1Ime() : "Igrac 1")
                        + ": " + partija.getBodovi1());
        binding.tvIgrac2.setText(
                (partija.getIgrac2Ime() != null ? partija.getIgrac2Ime() : "Igrac 2")
                        + ": " + partija.getBodovi2());
        binding.tvIgra.setText("Igra " + (partija.getTrenutnaIgra() + 1) + "/6");
    }

    private void pokrniIgru(int indeksIgre) {
        if (indeksIgre >= IGRE.length) {
            // Sve igre završene
            završiPartiju();
            return;
        }

        binding.tvIgra.setText("Igra " + (indeksIgre + 1) + "/6: " + IGRE[indeksIgre]);

        // TODO: U KO fazi ovdje navigiramo na konkretnu igru
        // Za sada prikazujemo Toast
        Toast.makeText(getContext(),
                "Pokretanje igre: " + IGRE[indeksIgre],
                Toast.LENGTH_SHORT).show();
    }

    private void završiPartiju() {
        if (trenutnaPartija == null) return;

        int bodovi1 = trenutnaPartija.getBodovi1();
        int bodovi2 = trenutnaPartija.getBodovi2();
        String pobjednikId;

        if (bodovi1 > bodovi2) {
            pobjednikId = trenutnaPartija.getIgrac1Id();
        } else if (bodovi2 > bodovi1) {
            pobjednikId = trenutnaPartija.getIgrac2Id();
        } else {
            pobjednikId = "nerijeseno";
        }

        partijaRepository.završiPartiju(partijaId, pobjednikId,
                () -> {
                    // Ažuriraj zvezde i tokene
                    String mojiId = authRepository.trenutniKorisnik().getUid();
                    boolean jePobjedio = pobjednikId.equals(mojiId);
                    int mojiBodyvi = jeIgrac1 ? bodovi1 : bodovi2;

                    // Bodovanje prema specifikaciji
                    int zvezdeDodati;
                    if (!trenutnaPartija.isPrijateljska()) {
                        if (jePobjedio) {
                            zvezdeDodati = 10 + (mojiBodyvi / 40);
                        } else {
                            zvezdeDodati = -10 + (mojiBodyvi / 40);
                        }
                        partijaRepository.azurirajZvezdeITokene(mojiId, zvezdeDodati,
                                () -> {}, poruka -> {});
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show());
    }

    private void prikaziRezultat(Partija partija) {
        String mojiId = authRepository.trenutniKorisnik().getUid();
        boolean jePobjedio = partija.getPobjednik() != null &&
                partija.getPobjednik().equals(mojiId);

        Toast.makeText(getContext(),
                jePobjedio ? "Pobjedili ste!" : "Izgubili ste!",
                Toast.LENGTH_LONG).show();

        // Vrati na Home
        Navigation.findNavController(requireView())
                .navigate(R.id.action_igraFragment_to_homeFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        partijaRepository.ukloniListener();
        binding = null;
    }
}