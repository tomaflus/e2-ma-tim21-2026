package com.elfak.slagalica.fragments.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentFriendsBinding;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.FriendRepository;
import com.elfak.slagalica.repository.PartijaRepository;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

public class FriendsFragment extends Fragment {
    private FragmentFriendsBinding binding;
    private FriendRepository friendRepository;
    private AuthRepository authRepository;
    private PartijaRepository partijaRepository;
    private FriendsAdapter adapter;
    private String mojeIme = "";

    // Inicijalizacija skenera
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    dodajPrijatelja(result.getContents());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        friendRepository = new FriendRepository();
        authRepository = new AuthRepository();
        partijaRepository = new PartijaRepository();

        adapter = new FriendsAdapter(new ArrayList<>(), receiver -> {
            if (authRepository.trenutniKorisnik() == null || getContext() == null) return;
            String ime = mojeIme.isEmpty() ? "Igrač" : mojeIme;
            partijaRepository.kreirajPrijateljskuPartiju(
                ime,
                gameId -> friendRepository.createInvite(
                    receiver.getId(), ime, gameId,
                    inviteId -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                getString(R.string.msg_invite_sent), Toast.LENGTH_SHORT).show();
                        }
                    },
                    err -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                "Greška pri slanju poziva", Toast.LENGTH_SHORT).show();
                        }
                    }
                ),
                err -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                            "Greška pri kreiranju partije", Toast.LENGTH_SHORT).show();
                    }
                }
            );
        });

        binding.rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFriends.setAdapter(adapter);

        ucitajPrijatelje();

        // Dugme za pretragu
        binding.btnSearch.setOnClickListener(v -> {
            String q = binding.etSearch.getText().toString();
            if (!q.isEmpty()) {
                friendRepository.searchUsers(q, users -> adapter.updateData(users));
            }
        });

        // Dugme za QR skeniranje
        binding.btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Skeniraj QR kod prijatelja");
            options.setBeepEnabled(false);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });
    }

    private void dodajPrijatelja(String friendId) {
        friendRepository.addFriendMutual(friendId, () -> {
            Toast.makeText(getContext(), "Prijatelj uspešno dodat!", Toast.LENGTH_SHORT).show();
            ucitajPrijatelje(); // Osvezi listu
        });
    }

    private void ucitajPrijatelje() {
        if (authRepository.trenutniKorisnik() == null) return;
        authRepository.getKorisnikPodaci(authRepository.trenutniKorisnik().getUid(), user -> {
            if (user != null && binding != null) {
                if (user.getKorisnickoIme() != null) mojeIme = user.getKorisnickoIme();
                friendRepository.loadFriends(user.getPrijateljiIds(), friends -> adapter.updateData(friends));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}