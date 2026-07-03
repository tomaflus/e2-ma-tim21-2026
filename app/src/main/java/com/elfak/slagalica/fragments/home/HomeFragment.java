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

import android.os.Handler;
import android.os.Looper;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentHomeBinding;
import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.elfak.slagalica.service.RewardService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static boolean toastPrikazan = false;

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

        prikaziNeprocitaneToast();

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

        binding.btnRangLista.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_rangListaFragment);
        });

        binding.btnDnevneMisije.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_dnevneMisijeFragment);
        });

        binding.btnTurnir.setOnClickListener(v -> {
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_turnirFragment);
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (userRepository != null) ucitajTokene();
        proveriNagrade();
    }

    private void proveriNagrade() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || !isAdded()) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifikacije")
                .whereEqualTo("tip", "nagrade")
                .whereEqualTo("procitana", false)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty() || !isAdded()) return;
                    com.google.firebase.firestore.DocumentSnapshot doc =
                            snapshots.getDocuments().get(0);
                    Notifikacija n = doc.toObject(Notifikacija.class);
                    if (n == null) return;

                    Bundle args = new Bundle();
                    args.putInt("tokeniNedelja", n.tokeniNedelja);
                    args.putInt("pozicijaNedelja", n.pozicijaNedelja);
                    args.putInt("tokeniMesec", n.tokeniMesec);
                    args.putInt("pozicijaMesec", n.pozicijaMesec);
                    args.putString("uid", uid);
                    args.putString("docId", doc.getId());

                    requireView().post(() -> {
                        if (isAdded()) {
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_homeFragment_to_nagradaFragment, args);
                        }
                    });
                });
    }

    private void prikaziNeprocitaneToast() {
        if (toastPrikazan) return;
        toastPrikazan = true;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifikacije")
                .whereEqualTo("procitana", false)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty() || !isAdded()) return;

                    List<com.google.firebase.firestore.DocumentSnapshot> nagrade = new ArrayList<>();
                    List<com.google.firebase.firestore.DocumentSnapshot> ostale = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snaps.getDocuments()) {
                        String tip = doc.getString("tip");
                        if ("nagrade".equals(tip)) nagrade.add(doc);
                        else ostale.add(doc);
                    }

                    // Nagrade: prikaži svaku posebno u nizu
                    Handler h = new Handler(Looper.getMainLooper());
                    for (int i = 0; i < nagrade.size(); i++) {
                        com.google.firebase.firestore.DocumentSnapshot doc = nagrade.get(i);
                        String sadrzaj = doc.getString("sadrzaj");
                        String naslov = doc.getString("naslov");
                        final String poruka = (naslov != null ? naslov : "Nagrada") +
                                (sadrzaj != null && !sadrzaj.isEmpty() ? ": " + sadrzaj : "");
                        h.postDelayed(() -> {
                            if (isAdded()) Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
                        }, (long) i * 3500);
                    }

                    // Ostale notifikacije: sažeto
                    if (!ostale.isEmpty()) {
                        long delay = (long) nagrade.size() * 3500;
                        String poruka = ostale.size() > 3
                                ? "Imate " + ostale.size() + " nepročitanih obavještenja."
                                : "Imate " + ostale.size() + " nepročitano" + (ostale.size() > 1 ? "h" : "") + " obavještenje.";
                        h.postDelayed(() -> {
                            if (isAdded()) Toast.makeText(getContext(), poruka, Toast.LENGTH_SHORT).show();
                        }, delay);
                    }
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
                                        "Tokeni: " + user.getTokeni() + " 🎟   Zvezde: " + user.getZvezde() + " ★");
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