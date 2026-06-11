package com.elfak.slagalica.fragments.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentProfilBinding;
import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.Region;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.elfak.slagalica.service.RegionService;
import com.elfak.slagalica.service.stats.StatsService;

public class ProfilFragment extends Fragment {
    private FragmentProfilBinding binding;
    private AuthRepository auth;
    private UserRepository userRepository;
    private StatsService stats;
    private RegionService regionService;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    userRepository.uploadSlikuBase64(requireContext(), uri, url -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Glide.with(this).load(url).circleCrop().into(binding.ivAvatar);
                            Toast.makeText(getContext(), "Avatar uspešno promenjen!", Toast.LENGTH_SHORT).show();
                        }
                    }, poruka -> {
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Greška: " + poruka, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = new AuthRepository();
        userRepository = new UserRepository();
        stats = new StatsService(requireContext());
        regionService = new RegionService();

        ucitajPodatke();
        updateStats();

        binding.btnFriends.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_profilFragment_to_friendsFragment));
        binding.btnRegions.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_profilFragment_to_regionsFragment));
        binding.btnLeagues.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_profilFragment_to_leaguesFragment));
        binding.btnChangeAvatar.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.btnLogout.setOnClickListener(v -> {
            auth.odjava();
            Navigation.findNavController(v).navigate(R.id.action_profilFragment_to_loginFragment);
        });
    }

    private void ucitajPodatke() {
        if (auth.trenutniKorisnik() != null) {
            String uid = auth.trenutniKorisnik().getUid();
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + uid;
            Glide.with(this).load(qrUrl).into(binding.ivQrCode);

            auth.getKorisnikPodaci(uid, (User user) -> {
                if (user != null && binding != null) {
                    binding.tvKorisnickoIme.setText(user.getKorisnickoIme());
                    binding.tvEmail.setText(user.getEmail());
                    binding.tvTokeni.setText(String.valueOf(user.getTokeni()));
                    binding.tvZvezde.setText(String.valueOf(user.getZvezde()));
                    binding.tvRegion.setText(user.getRegion());

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        // Glide automatski prepoznaje Base64 string
                        Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(binding.ivAvatar);
                    }

                    League league = League.getByStars(user.getZvezde());
                    binding.tvLiga.setText(league.getName());
                    
                    updateAvatarFrame(Region.getByName(user.getRegion()));
                }
            });
        }
    }

    private void updateAvatarFrame(Region userRegion) {
        regionService.calculateRegionalRanking(ranking -> {
            if (binding == null) return;
            int frameColor = R.color.no_rank;
            for (int i = 0; i < Math.min(3, ranking.size()); i++) {
                if (ranking.get(i).getKey() == userRegion) {
                    if (i == 0) frameColor = R.color.gold;
                    else if (i == 1) frameColor = R.color.silver;
                    else frameColor = R.color.bronze;
                    break;
                }
            }
            binding.ivAvatar.getBackground().setTint(getResources().getColor(frameColor, null));
        });
    }

    private void updateStats() {
        if (binding == null) return;
        binding.tvOdigranoIgaraVrednost.setText(getString(R.string.stat_games_played, stats.getGamesPlayed()));
        binding.tvProsecniBodovi.setText(getString(R.string.stat_avg_score, stats.getAvgScore()));
        binding.tvPobedaVrednost.setText(getString(R.string.stat_wins, stats.getWins(), stats.getWinRate()));
        binding.tvTacniKviza.setText(getString(R.string.stat_quiz_accuracy, stats.getQuizAccuracy()));
        binding.tvPocenatSpojnica.setText(getString(R.string.stat_matching_accuracy, stats.getMatchingAccuracy()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}