package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Detects a single transaction that exceeds a flat, absolute dollar limit
 * (e.g. > $10,000), independent of the account's own history - this is the
 * literal "Amount Threshold Rule" from the assignment spec.
 *
 * Deliberately separate from AmountAnomalyRule (which flags statistical
 * deviation from an account's own historical mean): a brand-new account
 * with no transaction history can never trigger AmountAnomalyRule (no
 * stddev to compute a z-score from), but a single large first transaction
 * must still be caught - that's exactly what this rule is for.
 *
 * DB-driven: rule.getThresholdValue() is the flat dollar limit.
 */
@Component
public class AmountThresholdRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        BigDecimal threshold = rule.getThresholdValue();
        BigDecimal amount = transaction.getAmount();

        boolean triggered = amount.compareTo(threshold) > 0;

        // Normalize: an amount at 2x the threshold or higher maxes out the score at 1.0.
        double ratio = amount.doubleValue() / threshold.doubleValue();
        double normalizedScore = triggered ? Math.max(0.0, Math.min(ratio / 2.0, 1.0)) : 0.0;

        String reason = triggered
                ? String.format("Transaction amount %.2f exceeded the flat threshold of %.2f", amount.doubleValue(), threshold.doubleValue())
                : String.format("Transaction amount %.2f is within the flat threshold of %.2f", amount.doubleValue(), threshold.doubleValue());

        return new RuleResult(getRuleType().name(), triggered, normalizedScore, reason);
    }
}
