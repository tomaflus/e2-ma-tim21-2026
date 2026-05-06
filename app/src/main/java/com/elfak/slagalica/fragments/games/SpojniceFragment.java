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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpojniceFragment extends Fragment {

    private FragmentSpojniceBinding binding;
    private CountDownTimer timer;
    private int playerScore = 0;
    private int opponentScore = 0;
    private int currentPairIndex = 0;
    
    private List<String> leftItems;
    private List<String> rightItems;
    private List<String> correctRightItems;
    private Button currentLeftButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSpojniceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadMockData();
        setupUI();
        startTimer();

        // KT1 Mockup for demo: Simulate end game
        binding.btnMockGameOver.setOnClickListener(v -> showGameOver());

        binding.btnBackToHome.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        binding.btnConfirm.setOnClickListener(v -> {
            // Mock functionality for KT1
            showGameOver();
        });
    }

    private void loadMockData() {
        leftItems = new ArrayList<>();
        leftItems.add("Srbija");
        leftItems.add("Francuska");
        leftItems.add("Nemačka");
        leftItems.add("Italija");
        leftItems.add("Španija");

        correctRightItems = new ArrayList<>();
        correctRightItems.add("Beograd");
        correctRightItems.add("Pariz");
        correctRightItems.add("Berlin");
        correctRightItems.add("Rim");
        correctRightItems.add("Madrid");

        rightItems = new ArrayList<>(correctRightItems);
        Collections.shuffle(rightItems);
    }

    private void setupUI() {
        binding.tvScorePlayer.setText(getString(R.string.label_score_player, playerScore));
        binding.tvScoreOpponent.setText(getString(R.string.label_score_opponent, opponentScore));
        binding.tvRound.setText(getString(R.string.label_round, 1));

        binding.btnLeft1.setText(leftItems.get(0));
        binding.btnLeft2.setText(leftItems.get(1));
        binding.btnLeft3.setText(leftItems.get(2));
        binding.btnLeft4.setText(leftItems.get(3));
        binding.btnLeft5.setText(leftItems.get(4));

        binding.btnRight1.setText(rightItems.get(0));
        binding.btnRight2.setText(rightItems.get(1));
        binding.btnRight3.setText(rightItems.get(2));
        binding.btnRight4.setText(rightItems.get(3));
        binding.btnRight5.setText(rightItems.get(4));

        View.OnClickListener leftClickListener = v -> {
            if (currentLeftButton != null) {
                currentLeftButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
            }
            currentLeftButton = (Button) v;
            currentLeftButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light, null));
        };

        binding.btnLeft1.setOnClickListener(leftClickListener);
        binding.btnLeft2.setOnClickListener(leftClickListener);
        binding.btnLeft3.setOnClickListener(leftClickListener);
        binding.btnLeft4.setOnClickListener(leftClickListener);
        binding.btnLeft5.setOnClickListener(leftClickListener);

        View.OnClickListener rightClickListener = v -> {
            if (currentLeftButton == null) return;

            Button rightButton = (Button) v;
            String leftText = currentLeftButton.getText().toString();
            String rightText = rightButton.getText().toString();

            int leftIdx = leftItems.indexOf(leftText);
            if (correctRightItems.get(leftIdx).equals(rightText)) {
                rightButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null));
                currentLeftButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null));
                playerScore += 5;
            } else {
                rightButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
                currentLeftButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
                opponentScore += 2;
            }

            currentLeftButton.setEnabled(false);
            rightButton.setEnabled(false);
            currentLeftButton = null;
            
            binding.tvScorePlayer.setText(getString(R.string.label_score_player, playerScore));
            binding.tvScoreOpponent.setText(getString(R.string.label_score_opponent, opponentScore));

            currentPairIndex++;
            if (currentPairIndex == 5) {
                showGameOver();
            }
        };

        binding.btnRight1.setOnClickListener(rightClickListener);
        binding.btnRight2.setOnClickListener(rightClickListener);
        binding.btnRight3.setOnClickListener(rightClickListener);
        binding.btnRight4.setOnClickListener(rightClickListener);
        binding.btnRight5.setOnClickListener(rightClickListener);
    }

    private void startTimer() {
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTimer.setText(getString(R.string.label_timer, (int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText(getString(R.string.label_timer, 0));
                showGameOver();
            }
        }.start();
    }

    private void showGameOver() {
        if (timer != null) timer.cancel();
        binding.clGameContent.setVisibility(View.GONE);
        binding.clGameOver.setVisibility(View.VISIBLE);
        binding.tvFinalScore.setText(getString(R.string.score_won, playerScore));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
        binding = null;
    }
}