package com.elfak.slagalica.fragments.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentIgraBinding;
import com.elfak.slagalica.model.Partija;
import com.elfak.slagalica.model.StatusPartije;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.IzazovRepository;
import com.elfak.slagalica.repository.PartijaRepository;
import com.elfak.slagalica.repository.UserRepository;

public class IgraFragment extends Fragment {

    private FragmentIgraBinding binding;
    private PartijaRepository partijaRepository;
    private AuthRepository authRepository;
    private UserRepository userRepository;

    private String partijaId;
    private boolean jeIgrac1;
    private Partija trenutnaPartija;
    private boolean partijaPrikazana = false;
    private boolean napustanjeUToku = false;

    private String izazovId;
    private boolean jeIzazov = false;
    private int ukupnoBodovi = 0;
    private int currentIndeksIgre = 0;

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
        userRepository = new UserRepository();

        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
            izazovId = getArguments().getString("izazovId");
            jeIzazov = getArguments().getBoolean("jeIzazov", false);
        }

        if (jeIzazov) {
            pokrniIzazovIgru(0);
        } else {
            partijaRepository.slušajPartiju(partijaId,
                    partija -> {
                        trenutnaPartija = partija;
                        azurirajUI(partija);

                        if (partija.getStatus() == StatusPartije.ZAVRSENA && !partijaPrikazana) {
                            partijaPrikazana = true;
                            prikaziRezultat(partija);
                        } else if (partija.getStatus() == StatusPartije.NAPUSTENA
                                && !napustanjeUToku) {
                            napustanjeUToku = true;
                            String mojiId = authRepository.trenutniKorisnik().getUid();
                            String napustioId = partija.getNapustioId();

                            if (napustioId != null && napustioId.equals(mojiId)) {
                                // Ja sam napustio — idi na Home tiho
                                if (isAdded() && getView() != null) {
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_igraFragment_to_homeFragment);
                                }
                            } else {
                                // Protivnik je napustio — ja pobjedujem
                                Toast.makeText(getContext(),
                                        "Protivnik je napustio partiju! Pobjedili ste!",
                                        Toast.LENGTH_LONG).show();
                                azurirajZvezdeINaHome(true, jeIgrac1 ?
                                        partija.getBodovi1() : partija.getBodovi2());
                            }
                        }
                    },
                    poruka -> Toast.makeText(getContext(),
                            "Greska: " + poruka, Toast.LENGTH_SHORT).show());

            pokrniIgru(0);
        }

        // Interceptuj Back dugme
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Napustiti igru?")
                                .setMessage("Ako napustite izgubićete partiju i bićete penalizovani žetonima!")
                                .setPositiveButton("Napusti", (dialog, which) -> {
                                    if (partijaId != null && !jeIzazov) {
                                        String mojiId = authRepository
                                                .trenutniKorisnik().getUid();
                                        partijaRepository.napustiPartiju(
                                                partijaId, mojiId,
                                                () -> {
                                                    if (isAdded() && getView() != null) {
                                                        requireActivity().runOnUiThread(() ->
                                                                Navigation.findNavController(requireView())
                                                                        .navigate(R.id.action_igraFragment_to_homeFragment));
                                                    }
                                                },
                                                poruka -> {});
                                    } else {
                                        if (isAdded() && getView() != null) {
                                            Navigation.findNavController(requireView())
                                                    .navigate(R.id.action_igraFragment_to_homeFragment);
                                        }
                                    }
                                })
                                .setNegativeButton("Nastavi igru", null)
                                .show();
                    }
                });
    }

    private void azurirajUI(Partija partija) {
        binding.tvIgrac1.setText(
                (partija.getIgrac1Ime() != null ? partija.getIgrac1Ime() : "Igrac 1")
                        + ": " + partija.getBodovi1());
        binding.tvIgrac2.setText(
                (partija.getIgrac2Ime() != null ? partija.getIgrac2Ime() : "Igrac 2")
                        + ": " + partija.getBodovi2());
        binding.tvIgra.setText("Igra " + (currentIndeksIgre + 1) + "/6: " + IGRE[currentIndeksIgre]);
    }

    private void pokrniIzazovIgru(int indeksIgre) {
        if (indeksIgre >= IGRE.length) {
            završiIzazov();
            return;
        }

        binding.tvIgra.setText("Igra " + (indeksIgre + 1) + "/6: " + IGRE[indeksIgre]);

        Bundle args = new Bundle();
        args.putString("izazovId", izazovId);
        args.putBoolean("jeIzazov", true);
        args.putBoolean("jeIgrac1", true);

        Fragment igra;
        switch (indeksIgre) {
            case 2:
                igra = new com.elfak.slagalica.fragments.games.AsocijacijeFragment();
                igra.setArguments(args);
                break;
            case 3:
                igra = new com.elfak.slagalica.fragments.games.SkockoFragment();
                igra.setArguments(args);
                break;
            case 4:
                igra = new com.elfak.slagalica.fragments.games.KorakPoKorakFragment();
                igra.setArguments(args);
                break;
            case 5:
                igra = new com.elfak.slagalica.fragments.games.MojBrojFragment();
                igra.setArguments(args);
                break;
            default:
                Toast.makeText(getContext(),
                        "Igra " + IGRE[indeksIgre] + " - dolazi uskoro",
                        Toast.LENGTH_SHORT).show();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(() -> pokrniIzazovIgru(indeksIgre + 1), 2000);
                return;
        }

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.gameContainer, igra)
                .commit();

        getChildFragmentManager().setFragmentResultListener(
                "asocijacijeZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    ukupnoBodovi += result.getInt("bodovi");
                    pokrniIzazovIgru(indeksIgre + 1);
                });

        getChildFragmentManager().setFragmentResultListener(
                "skockoZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    ukupnoBodovi += result.getInt("bodovi");
                    pokrniIzazovIgru(indeksIgre + 1);
                });

        getChildFragmentManager().setFragmentResultListener(
                "korakPoKorakZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    ukupnoBodovi += result.getInt("bodovi");
                    pokrniIzazovIgru(indeksIgre + 1);
                });

        getChildFragmentManager().setFragmentResultListener(
                "mojBrojZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    ukupnoBodovi += result.getInt("bodovi");
                    pokrniIzazovIgru(indeksIgre + 1);
                });
    }

    private void završiIzazov() {
        IzazovRepository izazovRepository = new IzazovRepository();
        izazovRepository.sacuvajRezultat(izazovId, ukupnoBodovi,
                id -> {
                    Toast.makeText(getContext(),
                            "Završili ste izazov sa " + ukupnoBodovi + " bodova!",
                            Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_igraFragment_to_homeFragment);
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show());
    }

    private void pokrniIgru(int indeksIgre) {
        if (indeksIgre >= IGRE.length) {
            završiPartiju();
            return;
        }

        currentIndeksIgre = indeksIgre;
        binding.tvIgra.setText("Igra " + (indeksIgre + 1) + "/6: " + IGRE[indeksIgre]);

        Bundle args = new Bundle();
        args.putString("partijaId", partijaId);
        args.putBoolean("jeIgrac1", jeIgrac1);

        Fragment igra;
        switch (indeksIgre) {
            case 2:
                igra = new com.elfak.slagalica.fragments.games.AsocijacijeFragment();
                igra.setArguments(args);
                break;
            case 3:
                igra = new com.elfak.slagalica.fragments.games.SkockoFragment();
                igra.setArguments(args);
                break;
            case 4:
                igra = new com.elfak.slagalica.fragments.games.KorakPoKorakFragment();
                igra.setArguments(args);
                break;
            case 5:
                igra = new com.elfak.slagalica.fragments.games.MojBrojFragment();
                igra.setArguments(args);
                break;
            default:
                Toast.makeText(getContext(),
                        "Igra " + IGRE[indeksIgre] + " - dolazi uskoro",
                        Toast.LENGTH_SHORT).show();
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(() -> pokrniIgru(indeksIgre + 1), 2000);
                return;
        }

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.gameContainer, igra)
                .commit();

        getChildFragmentManager().setFragmentResultListener(
                "asocijacijeZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    int bodovi = result.getInt("bodovi");
                    azurirajBodovePartije(bodovi, indeksIgre);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> pokrniIgru(indeksIgre + 1), 2000);
                });

        getChildFragmentManager().setFragmentResultListener(
                "skockoZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    int bodovi = result.getInt("bodovi");
                    azurirajBodovePartije(bodovi, indeksIgre);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> pokrniIgru(indeksIgre + 1), 2000);
                });

        getChildFragmentManager().setFragmentResultListener(
                "korakPoKorakZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    int bodovi = result.getInt("bodovi");
                    azurirajBodovePartije(bodovi, indeksIgre);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> pokrniIgru(indeksIgre + 1), 2000);
                });

        getChildFragmentManager().setFragmentResultListener(
                "mojBrojZavrsen", getViewLifecycleOwner(),
                (key, result) -> {
                    int bodovi = result.getInt("bodovi");
                    azurirajBodovePartije(bodovi, indeksIgre);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> pokrniIgru(indeksIgre + 1), 2000);
                });
    }

    private void azurirajBodovePartije(int noviBodovi, int indeksIgre) {
        if (trenutnaPartija == null) return;

        int ukupnoBodovi = jeIgrac1 ?
                trenutnaPartija.getBodovi1() + noviBodovi :
                trenutnaPartija.getBodovi2() + noviBodovi;

        partijaRepository.azurirajBodove(partijaId, jeIgrac1, ukupnoBodovi,
                () -> {},
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show());
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

        int mojiBodyvi = jeIgrac1 ? bodovi1 : bodovi2;
        String mojiId = authRepository.trenutniKorisnik().getUid();
        boolean jePobjedio = pobjednikId.equals(mojiId);

        partijaRepository.završiPartiju(partijaId, pobjednikId,
                () -> azurirajZvezdeINaHome(jePobjedio, mojiBodyvi),
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show());
    }

    private void azurirajZvezdeINaHome(boolean jePobjedio, int mojiBodyvi) {
        if (trenutnaPartija == null) return;

        userRepository.azurirajNakonPartije(
                jePobjedio,
                mojiBodyvi,
                trenutnaPartija.isPrijateljska(),
                () -> {
                    String poruka = jePobjedio ?
                            "Pobjedili ste! +zvezde" :
                            "Izgubili ste! -zvezde";
                    Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();

                    if (isAdded() && getView() != null) {
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_igraFragment_to_homeFragment);
                    }
                },
                poruka -> {
                    if (isAdded() && getView() != null) {
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_igraFragment_to_homeFragment);
                    }
                }
        );
    }

    private void prikaziRezultat(Partija partija) {
        String mojiId = authRepository.trenutniKorisnik().getUid();
        boolean jePobjedio = partija.getPobjednik() != null &&
                partija.getPobjednik().equals(mojiId);
        int mojiBodyvi = jeIgrac1 ?
                partija.getBodovi1() : partija.getBodovi2();

        azurirajZvezdeINaHome(jePobjedio, mojiBodyvi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        partijaRepository.ukloniListener();
        binding = null;
    }
}