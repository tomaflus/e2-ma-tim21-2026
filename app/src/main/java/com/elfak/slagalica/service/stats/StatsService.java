package com.elfak.slagalica.service.stats;

import android.content.Context;
import android.content.SharedPreferences;

public class StatsService {
    private final SharedPreferences prefs;

    public StatsService(Context context) {
        prefs = context.getSharedPreferences("SlagalicaStats", Context.MODE_PRIVATE);
    }

    public void addQuizStats(int correct, int wrong) {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("q_c", getQuizCorrect() + correct);
        ed.putInt("q_w", getQuizWrong() + wrong);
        ed.apply();
    }

    public void addMatchingStats(int success, int total) {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("m_s", getMatchingSuccess() + success);
        ed.putInt("m_t", getMatchingTotal() + total);
        ed.apply();
    }

    public void addGamePlayed(boolean win, int score) {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("g_p", getGamesPlayed() + 1);
        ed.putInt("total_score", getTotalScore() + score);
        if (win) ed.putInt("wins", getWins() + 1);
        else ed.putInt("losses", getLosses() + 1);
        ed.apply();
    }

    public int getQuizCorrect() { return prefs.getInt("q_c", 0); }
    public int getQuizWrong() { return prefs.getInt("q_w", 0); }
    public int getMatchingSuccess() { return prefs.getInt("m_s", 0); }
    public int getMatchingTotal() { return prefs.getInt("m_t", 0); }
    public int getGamesPlayed() { return prefs.getInt("g_p", 0); }
    public int getWins() { return prefs.getInt("wins", 0); }
    public int getLosses() { return prefs.getInt("losses", 0); }
    public int getTotalScore() { return prefs.getInt("total_score", 0); }

    public double getAvgScore() {
        int total = getGamesPlayed();
        return total == 0 ? 0 : (double) getTotalScore() / total;
    }

    public double getWinRate() {
        int total = getGamesPlayed();
        if (total == 0) return 0;
        return (double) getWins() / total * 100;
    }

    public double getQuizAccuracy() {
        int total = getQuizCorrect() + getQuizWrong();
        if (total == 0) return 0;
        return (double) getQuizCorrect() / total * 100;
    }

    public double getMatchingAccuracy() {
        int total = getMatchingTotal();
        if (total == 0) return 0;
        return (double) getMatchingSuccess() / total * 100;
    }
}