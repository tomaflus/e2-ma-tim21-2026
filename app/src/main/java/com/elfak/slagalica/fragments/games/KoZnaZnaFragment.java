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
import com.elfak.slagalica.databinding.FragmentKoZnaZnaBinding;

import java.util.ArrayList;
import java.util.List;

public class KoZnaZnaFragment extends Fragment {

    private FragmentKoZnaZnaBinding binding;
    private int currentQuestionIndex = 0;
    private int playerScore = 0;
    private int opponentScore = 0;
    private CountDownTimer timer;

    private static class Question {
        String text;
        String[] answers;
        int correctAnswerIndex;

        Question(String text, String[] answers, int correctAnswerIndex) {
            this.text = text;
            this.answers = answers;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }

    private List<Question> questions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKoZnaZnaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadMockQuestions();
        updateUI();

        View.OnClickListener answerClickListener = v -> {
            if (timer != null) timer.cancel();
            Button clickedButton = (Button) v;
            int clickedIndex = -1;
            if (v.getId() == R.id.btnAnswer1) clickedIndex = 0;
            else if (v.getId() == R.id.btnAnswer2) clickedIndex = 1;
            else if (v.getId() == R.id.btnAnswer3) clickedIndex = 2;
            else if (v.getId() == R.id.btnAnswer4) clickedIndex = 3;

            if (clickedIndex == questions.get(currentQuestionIndex).correctAnswerIndex) {
                clickedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null));
                playerScore += 10;
            } else {
                clickedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
                opponentScore += 5;
            }
            
            v.postDelayed(this::nextQuestion, 1000);
        };

        binding.btnAnswer1.setOnClickListener(answerClickListener);
        binding.btnAnswer2.setOnClickListener(answerClickListener);
        binding.btnAnswer3.setOnClickListener(answerClickListener);
        binding.btnAnswer4.setOnClickListener(answerClickListener);

        // KT1 Mockup for demo: Simulate end game
        binding.btnMockGameOver.setOnClickListener(v -> showGameOver());

        binding.btnBackToHome.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        startTimer();
    }

    private void loadMockQuestions() {
        questions = new ArrayList<>();
        questions.add(new Question("Glavni grad Srbije je?", new String[]{"Beograd", "Novi Sad", "Niš", "Kragujevac"}, 0));
        questions.add(new Question("Koja planeta je najbliža Suncu?", new String[]{"Venera", "Mars", "Merkur", "Zemlja"}, 2));
        questions.add(new Question("Koliko kontinenata postoji?", new String[]{"5", "6", "7", "8"}, 2));
        questions.add(new Question("Ko je napisao 'Na Drini ćuprija'?", new String[]{"Mesa Selimović", "Ivo Andrić", "Branko Ćopić", "Desanka Maksimović"}, 1));
        questions.add(new Question("Koji je hemijski simbol za zlato?", new String[]{"Ag", "Fe", "Au", "Cu"}, 2));
    }

    private void updateUI() {
        Question q = questions.get(currentQuestionIndex);
        binding.tvQuestion.setText(q.text);
        binding.btnAnswer1.setText(q.answers[0]);
        binding.btnAnswer2.setText(q.answers[1]);
        binding.btnAnswer3.setText(q.answers[2]);
        binding.btnAnswer4.setText(q.answers[3]);
        
        binding.tvQuestionNumber.setText(getString(R.string.label_question_count, currentQuestionIndex + 1));
        binding.tvScorePlayer.setText(getString(R.string.label_score_player, playerScore));
        binding.tvScoreOpponent.setText(getString(R.string.label_score_opponent, opponentScore));

        // Reset colors (assuming purple_500 is defined)
        int defaultColor = getResources().getColor(android.R.color.holo_purple, null);
        binding.btnAnswer1.setBackgroundColor(defaultColor);
        binding.btnAnswer2.setBackgroundColor(defaultColor);
        binding.btnAnswer3.setBackgroundColor(defaultColor);
        binding.btnAnswer4.setBackgroundColor(defaultColor);
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                binding.tvTimer.setText(getString(R.string.label_timer, seconds));
                binding.pbTimer.setProgress(seconds);
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText(getString(R.string.label_timer, 0));
                binding.pbTimer.setProgress(0);
                opponentScore += 5;
                nextQuestion();
            }
        }.start();
    }

    private void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            updateUI();
            startTimer();
        } else {
            showGameOver();
        }
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