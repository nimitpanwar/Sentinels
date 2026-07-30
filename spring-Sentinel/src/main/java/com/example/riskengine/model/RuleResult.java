package com.example.riskengine.model;

/**
 * Output of a single risk rule evaluation. Stored so the final RiskResult
 * can explain exactly which rules fired and why.
 * Ported from backend/'s com.frauddetection.model.RuleResult, unchanged.
 */
public class RuleResult {

    private final String ruleName;
    private final boolean triggered;
    private final double score; // normalized 0.0 - 1.0
    private final String reason;

    public RuleResult(String ruleName, boolean triggered, double score, String reason) {
        this.ruleName = ruleName;
        this.triggered = triggered;
        this.score = score;
        this.reason = reason;
    }

    public String getRuleName() { return ruleName; }
    public boolean isTriggered() { return triggered; }
    public double getScore() { return score; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("%-20s triggered=%-5s score=%.2f reason=%s", ruleName, triggered, score, reason);
    }
}
