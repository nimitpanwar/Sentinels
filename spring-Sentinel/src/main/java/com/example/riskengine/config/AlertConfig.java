package com.example.riskengine.config;

import com.example.enums.Severity;
import org.springframework.stereotype.Component;

/**
 * HARD-CODED alert/case configuration (severity bands, creation threshold,
 * merge/cooldown window). Per the user's decision, only rule-level
 * weights/thresholds are DB-driven (via the 'rules' table) - these
 * case/alert-level bands stay hardcoded since there's no config table for
 * them in the given schema.
 * Ported from backend/'s com.frauddetection.config.AlertConfig - severity
 * enum values renamed to match the schema's ENUM('HIGH','MID','LOW').
 */
@Component
public class AlertConfig {

    private final int minScoreToCreateAlert = 50; // scores below this are ignored, no alert created
    private final int lowSeverityMax = 60;          // 0-60   = LOW
    private final int mediumSeverityMax = 80;       // 61-80  = MID, 81-100 = HIGH
    private final int mergeCooldownMinutes = 60;    // window to merge new activity into an existing open case

    public int getMinScoreToCreateAlert() { return minScoreToCreateAlert; }
    public int getMergeCooldownMinutes() { return mergeCooldownMinutes; }

    public Severity severityFor(int riskScore) {
        if (riskScore <= lowSeverityMax) return Severity.LOW;
        if (riskScore <= mediumSeverityMax) return Severity.MID;
        return Severity.HIGH;
    }
}

