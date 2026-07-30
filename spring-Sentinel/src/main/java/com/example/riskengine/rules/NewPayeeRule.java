package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

/**
 * Detects payments to a payee the account has never transacted with before.
 * Ported from backend/'s com.frauddetection.rules.NewPayeeRule, unchanged logic.
 * DB-driven: rule.getThresholdValue() is the normalized score (0-1) contributed when triggered.
 */
@Component
public class NewPayeeRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.NEW_PAYEE;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        boolean known = profile.getKnownPayees().contains(transaction.getPayeeId());

        if (known) {
            return new RuleResult(getRuleType().name(), false, 0.0, "Payee has transacted with this account before");
        }

        return new RuleResult(getRuleType().name(), true, rule.getThresholdValue().doubleValue(),
                "First transaction to payee " + transaction.getPayeeId());
    }
}
