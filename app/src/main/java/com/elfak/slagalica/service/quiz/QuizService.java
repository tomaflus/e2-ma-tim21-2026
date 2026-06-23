package com.elfak.slagalica.service.quiz;

import com.elfak.slagalica.model.quiz.Question;
import com.elfak.slagalica.repository.quiz.QuizRepository;
import com.elfak.slagalica.service.stats.StatsService;
import java.util.Collections;
import java.util.List;

public class QuizService {
    private final List<Question> questions;
    private final StatsService stats;
    private int index = 0;
    private int score = 0;
    private int correct = 0;
    private int wrong = 0;

    public QuizService(QuizRepository repo, StatsService stats) {
        this.stats = stats;
        List<Question> all = repo.getMockQuestions();
        Collections.shuffle(all);
        this.questions = all.subList(0, Math.min(5, all.size()));
    }

    public Question getCurrent() {
        return index < questions.size() ? questions.get(index) : null;
    }

    public int getIndex() { return index; }
    public int getScore() { return score; }

    public int answer(int i) {
        if (index >= questions.size()) return 0;
        boolean isCorrect = (i == questions.get(index).getCorrectAnswerIndex());
        if (isCorrect) {
            score += 10;
            correct++;
            index++;
            return 10;
        } else {
            score -= 5;
            wrong++;
            index++;
            return -5;
        }
    }

    public void skip() { index++; }
    public boolean isOver() { return index >= questions.size(); }

    public void save() {
        stats.addQuizStats(correct, wrong);
        stats.addGamePlayed(score > 0, score);
    }
}