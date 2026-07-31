package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.repository.TransactionRepository;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Detects an unusual spike in transaction frequency within a configured
 * lookback window. The count is computed fresh per evaluation via
 * TransactionRepository, using the DB-driven Rule row's timeline (days) as
 * the window and thresholdValue as the trigger count (see entity/Rule.java).
 * Ported from backend/'s com.frauddetection.rules.VelocityAnomalyRule, unchanged logic.
 */
@Component
public class VelocityAnomalyRule implements RiskRule {

    private final TransactionRepository transactionRepository;

    public VelocityAnomalyRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.VELOCITY;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        LocalDateTime now = transaction.getTransactionTimestamp();
        LocalDateTime from = now.minusDays(Math.max(1, rule.getTimeline()));
        // COUNT query, not a fetch-then-size(): avoids loading full Transaction
        // rows (with their EAGER account/payee joins) just to count them - this
        // runs on every single evaluation, so it needs to stay a lightweight SQL
        // COUNT(*), never a cache (velocity must always reflect the true count).
        long count = transactionRepository
                .countByAccountAccountIdAndTransactionTimestampBetween(transaction.getAccountId(), from, now);

        int threshold = rule.getThresholdValue().intValue();

        boolean triggered = count >= threshold;
        double normalizedScore = Math.max(0.0, Math.min(count / (threshold * 2.0), 1.0));

        String reason = String.format("%d transactions detected within the last %d day(s) (threshold=%d)",
                count, rule.getTimeline(), threshold);

        return new RuleResult(getRuleType().name(), triggered, normalizedScore, reason);
    }
}
