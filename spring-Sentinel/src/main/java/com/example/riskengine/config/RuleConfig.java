package com.example.riskengine.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HARD-CODED rule configuration. In production these values would be
 * stored in a database and editable by fraud analysts through an admin UI.
 * Ported from backend/'s com.frauddetection.config.RuleConfig -
 * DEVICE_CHANGE entries removed, deviceId/DeviceChangeRule no longer exist.
 */
@Component
public class RuleConfig {

    /** Rule name -> enabled/disabled. HARD-CODED - would come from DB/admin UI. */
    private final Map<String, Boolean> enabled = new HashMap<>();
    /** Rule name -> weight/contribution towards the final score. HARD-CODED. */
    private final Map<String, Double> weights = new HashMap<>();

    // ---- Rule-specific hard-coded thresholds ----
    private final double amountZScoreThreshold = 3.0;  // z-score above this = anomaly
    private final int velocityThresholdCount = 5;       // txs within window to be "high velocity"
    private final int velocityWindowMinutes = 10;        // configurable lookback window
    private final double newPayeeRiskScore = 0.8;        // base risk contribution for a first-time payee
    private final int lookbackDays = 90;                 // historical lookback period

    public RuleConfig() {
        enabled.put("AMOUNT_ANOMALY", true);
        enabled.put("VELOCITY_ANOMALY", true);
        enabled.put("NEW_PAYEE", true);
        enabled.put("TRANSACTION_TIME", true);
        enabled.put("LOCATION_CHANGE", true);
        enabled.put("SPENDING_PATTERN", true);

        weights.put("AMOUNT_ANOMALY", 1.0);
        weights.put("VELOCITY_ANOMALY", 1.0);
        weights.put("NEW_PAYEE", 1.0);
        weights.put("TRANSACTION_TIME", 0.5);
        weights.put("LOCATION_CHANGE", 0.75);
        weights.put("SPENDING_PATTERN", 0.5);
    }

    public boolean isEnabled(String ruleName) {
        return enabled.getOrDefault(ruleName, false);
    }

    public double getWeight(String ruleName) {
        return weights.getOrDefault(ruleName, 0.0);
    }

    public double getAmountZScoreThreshold() { return amountZScoreThreshold; }
    public int getVelocityThresholdCount() { return velocityThresholdCount; }
    public int getVelocityWindowMinutes() { return velocityWindowMinutes; }
    public double getNewPayeeRiskScore() { return newPayeeRiskScore; }
    public int getLookbackDays() { return lookbackDays; }
}
