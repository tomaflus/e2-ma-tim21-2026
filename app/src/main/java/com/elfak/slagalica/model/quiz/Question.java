package com.elfak.slagalica.model.quiz;

public class Question {
    private String text;
    private String[] answers;
    private int correctAnswerIndex;
    private Difficulty difficulty;

    public Question() {}

    public Question(String text, String[] answers, int correctAnswerIndex, Difficulty difficulty) {
        this.text = text;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
        this.difficulty = difficulty;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String[] getAnswers() { return answers; }
    public void setAnswers(String[] answers) { this.answers = answers; }

    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
}