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
import com.elfak.slagalica.model.quiz.Question;
import com.elfak.slagalica.repository.quiz.QuizRepository;
import com.elfak.slagalica.service.quiz.QuizService;
import com.elfak.slagalica.service.stats.StatsService;

public class KoZnaZnaFragment extends Fragment {
    private FragmentKoZnaZnaBinding binding;
    private QuizService service;
    private CountDownTimer timer;
    private boolean isGameOver = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKoZnaZnaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        service = new QuizService(new QuizRepository(), new StatsService(requireContext()));
        
        View.OnClickListener listener = v -> {
            int i = (v.getId() == R.id.btnAnswer1) ? 0 : (v.getId() == R.id.btnAnswer2) ? 1 : (v.getId() == R.id.btnAnswer3) ? 2 : 3;
            processAnswer(i, (Button) v);
        };
        binding.btnAnswer1.setOnClickListener(listener);
        binding.btnAnswer2.setOnClickListener(listener);
        binding.btnAnswer3.setOnClickListener(listener);
        binding.btnAnswer4.setOnClickListener(listener);
        
        binding.btnMockGameOver.setOnClickListener(v -> finishGame());
        binding.btnBackToHome.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        
        render();
    }

    private void processAnswer(int i, Button b) {
        if (timer != null) timer.cancel();
        setButtonsEnabled(false);
        int points = service.answer(i);
        boolean ok = points > 0;
        b.setBackgroundColor(getResources().getColor(ok ? R.color.zelenaTacno : R.color.crvenaNetacno, null));
        b.postDelayed(this::render, 1000);
    }

    private void render() {
        if (binding == null) return;
        if (service.isOver()) {
            finishGame();
            return;
        }
        Question q = service.getCurrent();
        binding.tvQuestion.setText(q.getText());
        binding.btnAnswer1.setText(q.getAnswers()[0]);
        binding.btnAnswer2.setText(q.getAnswers()[1]);
        binding.btnAnswer3.setText(q.getAnswers()[2]);
        binding.btnAnswer4.setText(q.getAnswers()[3]);

        binding.tvQuestionNumber.setText(getString(R.string.label_question_count, service.getIndex() + 1));
        binding.tvScorePlayer.setText(getString(R.string.label_score_format, service.getScore()));
        binding.tvScoreOpponent.setText(getString(R.string.label_score_format, 0));
        
        resetButtons();
        setButtonsEnabled(true);
        startTimer();
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnAnswer1.setEnabled(enabled);
        binding.btnAnswer2.setEnabled(enabled);
        binding.btnAnswer3.setEnabled(enabled);
        binding.btnAnswer4.setEnabled(enabled);
    }

    private void resetButtons() {
        int c = getResources().getColor(R.color.purple_500, null);
        binding.btnAnswer1.setBackgroundColor(c);
        binding.btnAnswer2.setBackgroundColor(c);
        binding.btnAnswer3.setBackgroundColor(c);
        binding.btnAnswer4.setBackgroundColor(c);
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(5000, 1000) {
            public void onTick(long m) { 
                if (binding != null) {
                    binding.tvTimer.setText(getString(R.string.label_timer, m / 1000)); 
                    binding.pbTimer.setProgress((int)(m/1000));
                }
            }
            public void onFinish() { 
                if (binding != null) {
                    setButtonsEnabled(false);
                    service.skip(); 
                    render(); 
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