package com.elfak.slagalica.service.matching;

import com.elfak.slagalica.model.matching.MatchingPair;
import com.elfak.slagalica.repository.matching.MatchingRepository;
import com.elfak.slagalica.service.stats.StatsService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MatchingService {
    private final MatchingRepository repo;
    private final StatsService stats;
    private final List<List<MatchingPair>> gameSets;
    private List<MatchingPair> currentPairs;
    private List<String> shuffledRight;
    private final Set<String> matchedLeft = new HashSet<>();
    private int round = 1;
    private int score = 0;
    private int totalSuccess = 0;

    public MatchingService(MatchingRepository repo, StatsService stats) {
        this.repo = repo;
        this.stats = stats;
        this.gameSets = repo.getRandomGameSets(2);
        loadRound(1);
    }

    public void loadRound(int r) {
        this.round = r;
        this.matchedLeft.clear();
        this.currentPairs = gameSets.get(r - 1);
        shuffledRight = new ArrayList<>();
        for (MatchingPair p : currentPairs) shuffledRight.add(p.getRightTerm());
        Collections.shuffle(shuffledRight);
    }

    public boolean check(String left, String right) {
        if (matchedLeft.contains(left)) return false;
        for (MatchingPair p : currentPairs) {
            if (p.getLeftTerm().equals(left)) {
                if (p.getRightTerm().equals(right)) {
                    score += 2;
                    totalSuccess++;
                    matchedLeft.add(left);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasNext() { return round < 2; }
    public boolean isRoundOver() { return matchedLeft.size() >= (currentPairs != null ? currentPairs.size() : 0); }
    public int getScore() { return score; }
    public int getRound() { return round; }
    public List<MatchingPair> getPairs() { return currentPairs; }
    public List<String> getShuffledRight() { return shuffledRight; }
    public Set<String> getMatchedLeft() { return matchedLeft; }

    public void save() {
        stats.addMatchingStats(totalSuccess, 10);
        stats.addGamePlayed(score > 0, score);
    }
}