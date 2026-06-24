package com.elfak.slagalica.fragments.home;

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
import com.elfak.slagalica.databinding.FragmentHomeBinding;
import com.elfak.slagalica.helpers.NotifikacijaHelper;
import com.elfak.slagalica.model.NagradaInfo;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.elfak.slagalica.service.RewardService;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();
        userRepository = new UserRepository();

        proveriNagrade(view);

        // Dodaj dnevne tokene pri svakom otvaranju Home ekrana
        userRepository.dodajDnevneTokene(
                () -> ucitajTokene(),
                poruka -> ucitajTokene()
        );

        // Klik na Odjavi se
        binding.btnOdjava.setOnClickListener(v -> {
            authRepository.odjava();
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_loginFragment);
        });

        // Klik na Promijeni lozinku
        binding.btnPromjeniLozinku.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_resetLozinkeFragment);
        });

        binding.btnIgraj.setOnClickListener(v -> {
            if (authRepository.trenutniKorisnik() == null) {
                Toast.makeText(getContext(),
                        "Niste ulogovani!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Onemogući dugme da ne može kliknuti dvaput
            binding.btnIgraj.setEnabled(false);

            userRepository.oduzmiToken(
                    () -> {
                        // Vrati se na UI thread za navigaciju
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ucitajTokene();
                                binding.btnIgraj.setEnabled(true);
                                Navigation.findNavController(requireView())
                                        .navigate(R.id.action_homeFragment_to_cekanjeFragment);
                            });
                        }
                    },
                    poruka -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnIgraj.setEnabled(true);
                                Toast.makeText(getContext(),
                                        poruka, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
            );
        });


        binding.btnCet.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_cetFragment);
        });

        binding.btnIzazov.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_izazovFragment);
        });

        binding.btnNotifikacije.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_notifikacijeFragment);
        });

        // Student 2 Dugmad
        binding.btnProfil.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_profilFragment);
        });

        binding.btnKoZnaZna.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_koZnaZnaFragment);
        });

        binding.btnSpojnice.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_spojniceFragment);
        });

        binding.btnRangLista.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_rangListaFragment);
        });

        binding.btnDnevneMisije.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_dnevneMisijeFragment);
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (userRepository != null) ucitajTokene();
    }

    private void proveriNagrade(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new RewardService().proveriINagradi(requireContext(), uid, info -> {
            if (info == null || !isAdded()) return;
            Bundle args = new Bundle();
            args.putInt("tokeniNedelja", info.tokeniNedelja);
            args.putInt("pozicijaNedelja", info.pozicijaNedelja);
            args.putInt("tokeniMesec", info.tokeniMesec);
            args.putInt("pozicijaMesec", info.pozicijaMesec);
            view.post(() -> {
                if (isAdded()) {
                    Navigation.findNavController(view)
                            .navigate(R.id.action_homeFragment_to_nagradaFragment, args);
                }
            });
        });
    }

    private void ucitajTokene() {
        userRepository.dohvatiKorisnika(
                user -> {
                    if (getActivity() != null && binding != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.tvKorisnik.setText(user.getKorisnickoIme());
                                binding.tvTokeniZvezde.setText(
                                        "Tokeni: " + user.getTokeni() +
                                                " | Zvezde: " + user.getZvezde());
                            }
                        });
                    }
                },
                poruka -> {}
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}