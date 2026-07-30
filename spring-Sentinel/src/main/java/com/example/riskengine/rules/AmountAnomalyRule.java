package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Detects transactions whose amount deviates significantly from the
 * account's historical average (Z-score based).
 * Ported from backend/'s com.frauddetection.rules.AmountAnomalyRule, unchanged logic.
 * DB-driven: rule.getThresholdValue() is the z-score threshold (see entity/Rule.java).
 */
@Component
public class AmountAnomalyRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.AMOUNT_ANOMALY;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        BigDecimal stdDev = profile.getStdDevAmount();
        BigDecimal mean = profile.getMeanAmount();

        // Graceful handling of missing/insufficient history (cold start):
        // no meaningful standard deviation means we cannot compute a
        // z-score, so the rule is skipped instead of dividing by zero.
        if (stdDev == null || stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return new RuleResult(getRuleType().name(), false, 0.0,
                    "Insufficient history to compute amount deviation");
        }

        double zScore = transaction.getAmount().subtract(mean)
                .divide(stdDev, MathContext.DECIMAL64)
                .doubleValue();

        double threshold = rule.getThresholdValue().doubleValue();
        // Normalize: 3x the configured threshold maxes out the score at 1.0.
        double normalizedScore = Math.max(0.0, Math.min(zScore / (threshold * 3), 1.0));
        boolean triggered = zScore >= threshold;

        String reason = String.format("Transaction amount %.2f was %.1f standard deviations from the mean (%.2f)",
                transaction.getAmount().doubleValue(), zScore, mean.doubleValue());

        return new RuleResult(getRuleType().name(), triggered, normalizedScore, reason);
    }
}
