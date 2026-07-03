package com.elfak.slagalica.fragments.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.UserRepository;
import com.elfak.slagalica.service.LeagueService;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class LeaguesFragment extends Fragment {

    private UserRepository userRepository;
    private LeagueService leagueService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leagues, container, false);
        userRepository = new UserRepository();
        leagueService = new LeagueService(requireContext());

        view.findViewById(R.id.btnBack).setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.btnInfo).setOnClickListener(v -> showInfoSheet());

        refreshUserData(view);

        setupDebugButtons(view);

        return view;
    }

    private void refreshUserData(View view) {
        userRepository.dohvatiKorisnika(user -> {
            this.currentUser = user;
            updateUI(view, user);
        }, error -> {});
    }

    private void updateUI(View view, User user) {
        if (view == null) return;
        
        League currentLeague = League.getByStars(user.getZvezde());
        
        // Header Card
        ((ImageView)view.findViewById(R.id.ivCurrentLeagueIcon)).setImageResource(currentLeague.getIconRes());
        ((TextView)view.findViewById(R.id.tvCurrentLeagueName)).setText(currentLeague.getName().toUpperCase());
        ((TextView)view.findViewById(R.id.tvLeagueRank)).setText((currentLeague.ordinal() + 1) + " od 6");
        
        int nextLeagueIndex = currentLeague.ordinal() + 1;
        League nextLeague = nextLeagueIndex < League.values().length ? League.values()[nextLeagueIndex] : null;
        
        int currentStars = user.getZvezde();
        int minStars = currentLeague.getMinStars();
        int maxStars = nextLeague != null ? nextLeague.getMinStars() : currentStars;
        
        String starsText = currentStars + " / " + (nextLeague != null ? nextLeague.getMinStars() : "MAX");
        ((TextView)view.findViewById(R.id.tvCurrentStars)).setText(starsText);
        
        ProgressBar pb = view.findViewById(R.id.pbLeagueProgress);
        if (nextLeague != null) {
            // Dynamic progress calculation as requested
            int progress;
            if (maxStars == minStars) {
                progress = 100;
            } else {
                progress = (int) (((float)(currentStars - minStars) / (maxStars - minStars)) * 100);
            }
            pb.setProgress(Math.max(5, Math.min(100, progress))); 
            
            ((TextView)view.findViewById(R.id.tvNextLeagueInfo)).setText("Još " + (maxStars - currentStars) + " zvezda do " + nextLeague.getName() + " lige");
            ((TextView)view.findViewById(R.id.tvNextBonus)).setText("+1");
        } else {
            pb.setProgress(100);
            ((TextView)view.findViewById(R.id.tvNextLeagueInfo)).setText("Dostigli ste najvišu ligu!");
            view.findViewById(R.id.tvNextBonus).setVisibility(View.INVISIBLE);
        }

        ((TextView)view.findViewById(R.id.tvDailyTokens)).setText(String.valueOf(5 + currentLeague.ordinal()));
        ((TextView)view.findViewById(R.id.tvTokenBreakdown)).setText("5 osnovnih + " + currentLeague.ordinal() + " bonus");

        // Leagues List
        LinearLayout list = view.findViewById(R.id.llLeaguesList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (League l : League.values()) {
            View item = inflater.inflate(R.layout.item_league, list, false);
            
            TextView tvName = item.findViewById(R.id.tvLeagueName);
            TextView tvStars = item.findViewById(R.id.tvLeagueStars);
            TextView tvTokens = item.findViewById(R.id.tvLeagueTokens);
            ImageView ivIcon = item.findViewById(R.id.ivLeagueIcon);
            ImageView ivStatus = item.findViewById(R.id.ivStatus);

            tvName.setText((l.ordinal() + 1) + ". " + l.getName().toUpperCase());
            tvStars.setText("Potrebno zvezda: " + l.getMinStars());
            tvTokens.setText("Bonus tokeni: +" + l.ordinal());
            ivIcon.setImageResource(l.getIconRes());

            if (l.ordinal() < currentLeague.ordinal()) {
                ivStatus.setImageResource(R.drawable.ic_check);
                ivStatus.setColorFilter(0xFF4CAF50);
                item.setAlpha(0.8f);
            } else if (l.ordinal() == currentLeague.ordinal()) {
                ivStatus.setImageResource(R.drawable.ic_check);
                ivStatus.setColorFilter(0xFFFFFFFF);
                item.findViewById(R.id.cvLeagueItem).setBackgroundColor(0xFF2E2E4E);
            } else {
                ivStatus.setImageResource(R.drawable.ic_lock);
                ivStatus.setColorFilter(0xFF404040);
                item.setAlpha(0.5f);
            }

            list.addView(item);
        }
    }

    private void showInfoSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_league_info_sheet, null);
        sheetView.findViewById(R.id.btnCloseSheet).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void setupDebugButtons(View view) {
        view.findViewById(R.id.btnDebugAddStars).setOnClickListener(v -> {
            if (currentUser == null) return;
            int newStars = currentUser.getZvezde() + 50;
            updateUserStars(newStars, view);
        });

        view.findViewById(R.id.btnDebugSubStars).setOnClickListener(v -> {
            if (currentUser == null) return;
            int newStars = Math.max(0, currentUser.getZvezde() - 50);
            updateUserStars(newStars, view);
        });

        view.findViewById(R.id.btnDebugMonthlyReset).setOnClickListener(v -> {
            if (currentUser == null) return;
            int newStars = (int) (currentUser.getZvezde() * 0.7);
            updateUserStars(newStars, view);
            Toast.makeText(getContext(), "Monthly reset simulated (-30% stars)", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserStars(int newStars, View view) {
        userRepository.azurirajZvezdeILigu(newStars, () -> {
            // First update local state so UI reacts instantly
            currentUser.setZvezde(newStars);
            
            // Trigger promotion/demotion dialog check
            leagueService.checkLeagueTransition(currentUser);
            
            // Sync current user's league field locally after DB update
            currentUser.setLiga(League.getByStars(newStars).ordinal());
            
            // Refresh UI
            updateUI(view, currentUser);
        });
    }
}
