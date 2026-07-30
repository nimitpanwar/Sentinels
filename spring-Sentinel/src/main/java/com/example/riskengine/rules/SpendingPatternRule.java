package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

/**
 * Detects a change in normal merchant-category spending behaviour.
 * Ported from backend/'s com.frauddetection.rules.SpendingPatternRule, unchanged logic.
 * DB-driven: rule.getThresholdValue() is the normalized score (0-1) contributed when triggered.
 */
@Component
public class SpendingPatternRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.SPENDING_PATTERN;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        String category = transaction.getMerchantCategory();

        if (category == null || category.isBlank()) {
            return new RuleResult(getRuleType().name(), false, 0.0, "Merchant category unavailable");
        }

        boolean known = profile.getKnownMerchantCategories().contains(category);
        if (known) {
            return new RuleResult(getRuleType().name(), false, 0.0, "Category matches normal spending pattern: " + category);
        }

        return new RuleResult(getRuleType().name(), true, rule.getThresholdValue().doubleValue(), "Unusual merchant category: " + category);
    }
}
