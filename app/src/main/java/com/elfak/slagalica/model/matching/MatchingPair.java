package com.elfak.slagalica.model.matching;

public class MatchingPair {
    private String leftTerm;
    private String rightTerm;

    public MatchingPair() {}

    public MatchingPair(String leftTerm, String rightTerm) {
        this.leftTerm = leftTerm;
        this.rightTerm = rightTerm;
    }

    public String getLeftTerm() { return leftTerm; }
    public void setLeftTerm(String leftTerm) { this.leftTerm = leftTerm; }

    public String getRightTerm() { return rightTerm; }
    public void setRightTerm(String rightTerm) { this.rightTerm = rightTerm; }
}