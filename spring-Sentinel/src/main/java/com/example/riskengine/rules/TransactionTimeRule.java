package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

/**
 * Detects a transaction occurring outside the account's normal active hours.
 * Ported from backend/'s com.frauddetection.rules.TransactionTimeRule.
 * ADAPTED: reads transaction.getTransactionTimestamp() (LocalDateTime,
 * mapped directly from the MySQL TIMESTAMP column - no Instant conversion
 * needed with the real relational schema).
 * DB-driven: rule.getThresholdValue() is the normalized score (0-1) contributed when triggered.
 */
@Component
public class TransactionTimeRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.TIME_ANOMALY;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        int hour = transaction.getTransactionTimestamp().getHour();
        boolean withinNormalHours = hour >= profile.getNormalStartHour() && hour < profile.getNormalEndHour();

        if (withinNormalHours) {
            return new RuleResult(getRuleType().name(), false, 0.0, "Transaction occurred within normal hours");
        }

        return new RuleResult(getRuleType().name(), true, rule.getThresholdValue().doubleValue(),
                String.format("Transaction occurred at %02d:00, outside normal hours %02d:00-%02d:00",
                        hour, profile.getNormalStartHour(), profile.getNormalEndHour()));
    }
}
