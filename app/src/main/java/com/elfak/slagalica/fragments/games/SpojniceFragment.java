package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentSpojniceBinding;
import com.elfak.slagalica.repository.matching.MatchingRepository;
import com.elfak.slagalica.service.matching.MatchingService;
import com.elfak.slagalica.service.stats.StatsService;

public class SpojniceFragment extends Fragment {
    private FragmentSpojniceBinding binding;
    private MatchingService service;
    private CountDownTimer timer;
    private Button selectedLeft;
    private boolean isGameOver = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSpojniceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        service = new MatchingService(new MatchingRepository(), new StatsService(requireContext()));
        
        binding.btnMockGameOver.setOnClickListener(v -> finishGame());
        binding.btnBackToHome.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        binding.btnConfirm.setOnClickListener(v -> {
             if (service.getRound() < 2) {
                service.loadRound(2);
                render();
            } else {
                finishGame();
            }
        });

        setup(); 
        render();
    }

    private void setup() {
        View.OnClickListener leftL = v -> {
            if (selectedLeft != null) {
                selectedLeft.setBackgroundColor(getResources().getColor(R.color.sivaNeaktivno, null));
            }
            selectedLeft = (Button) v;
            selectedLeft.setBackgroundColor(getResources().getColor(R.color.plavaSelektovano, null));
        };
        binding.btnLeft1.setOnClickListener(leftL);
        binding.btnLeft2.setOnClickListener(leftL);
        binding.btnLeft3.setOnClickListener(leftL);
        binding.btnLeft4.setOnClickListener(leftL);
        binding.btnLeft5.setOnClickListener(leftL);

        View.OnClickListener rightL = v -> {
            if (selectedLeft == null) return;
            Button rightB = (Button) v;
            if (service.check(selectedLeft.getText().toString(), rightB.getText().toString())) {
                selectedLeft.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
                rightB.setBackgroundColor(getResources().getColor(R.color.zelenaTacno, null));
                selectedLeft.setEnabled(false); 
                rightB.setEnabled(false);
                selectedLeft = null;
                updateScores();
                if (service.isRoundOver()) {
                    binding.btnConfirm.setVisibility(View.VISIBLE);
                }
            } else {
                rightB.setBackgroundColor(getResources().getColor(R.color.crvenaNetacno, null));
                rightB.postDelayed(() -> {
                    if (binding != null) {
                        rightB.setBackgroundColor(getResources().getColor(R.color.purple_500, null));
                    }
                }, 500);
            }
        };
        binding.btnRight1.setOnClickListener(rightL);
        binding.btnRight2.setOnClickListener(rightL);
        binding.btnRight3.setOnClickListener(rightL);
        binding.btnRight4.setOnClickListener(rightL);
        binding.btnRight5.setOnClickListener(rightL);
    }

    private void render() {
        if (binding == null) return;
        selectedLeft = null;
        binding.btnConfirm.setVisibility(View.GONE);
        
        updateScores();
        
        binding.btnLeft1.setText(service.getPairs().get(0).getLeftTerm());
        binding.btnLeft2.setText(service.getPairs().get(1).getLeftTerm());
        binding.btnLeft3.setText(service.getPairs().get(2).getLeftTerm());
        binding.btnLeft4.setText(service.getPairs().get(3).getLeftTerm());
        binding.btnLeft5.setText(service.getPairs().get(4).getLeftTerm());

        binding.btnRight1.setText(service.getShuffledRight().get(0));
        binding.btnRight2.setText(service.getShuffledRight().get(1));
        binding.btnRight3.setText(service.getShuffledRight().get(2));
        binding.btnRight4.setText(service.getShuffledRight().get(3));
        binding.btnRight5.setText(service.getShuffledRight().get(4));
        
        binding.btnLeft1.setEnabled(true);
        binding.btnLeft2.setEnabled(true);
        binding.btnLeft3.setEnabled(true);
        binding.btnLeft4.setEnabled(true);
        binding.btnLeft5.setEnabled(true);
        binding.btnRight1.setEnabled(true);
        binding.btnRight2.setEnabled(true);
        binding.btnRight3.setEnabled(true);
        binding.btnRight4.setEnabled(true);
        binding.btnRight5.setEnabled(true);

        resetButtonColors();
        
        binding.tvRound.setText(getString(R.string.label_round, service.getRound()));
        startTimer();
    }

    private void updateScores() {
        binding.tvScorePlayer.setText(getString(R.string.label_score_format, service.getScore()));
        binding.tvScoreOpponent.setText(getString(R.string.label_score_format, 0));
    }

    private void resetButtonColors() {
        int leftC = getResources().getColor(R.color.sivaNeaktivno, null);
        int rightC = getResources().getColor(R.color.purple_500, null);
        
        binding.btnLeft1.setBackgroundColor(leftC);
        binding.btnLeft2.setBackgroundColor(leftC);
        binding.btnLeft3.setBackgroundColor(leftC);
        binding.btnLeft4.setBackgroundColor(leftC);
        binding.btnLeft5.setBackgroundColor(leftC);
        
        binding.btnRight1.setBackgroundColor(rightC);
        binding.btnRight2.setBackgroundColor(rightC);
        binding.btnRight3.setBackgroundColor(rightC);
        binding.btnRight4.setBackgroundColor(rightC);
        binding.btnRight5.setBackgroundColor(rightC);
    }
    
    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(30000, 1000) {
            public void onTick(long m) { 
                if (binding != null) binding.tvTimer.setText(getString(R.string.label_timer, m / 1000)); 
            }
            public void onFinish() { 
                if (binding != null) {
                    if (service.getRound() < 2) {
                        service.loadRound(2);
                        render();
                    } else {
                        finishGame();
                    }
                }
            }
        }.start();
    }

    private void finishGame() {
        if (isGameOver || binding == null) return;
        isGameOver = true;
        if (timer != null) timer.cancel();
        service.save(); 
        binding.clGameContent.setVisibility(View.GONE); 
        binding.clGameOver.setVisibility(View.VISIBLE); 
        binding.tvFinalScore.setText(getString(R.string.score_won, service.getScore()));
    }

    @Override
    public void onDestroyView() { 
        super.onDestroyView(); 
        if (timer != null) timer.cancel(); 
        binding = null;
    }
}