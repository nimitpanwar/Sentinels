package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;
import org.springframework.stereotype.Component;

/**
 * Detects transactions from an unusual location.
 *
 * IMPORTANT: location data is often unavailable. A missing location must
 * NOT break evaluation - the rule simply contributes 0 risk.
 * Ported from backend/'s com.frauddetection.rules.LocationChangeRule, unchanged logic.
 * DB-driven: rule.getThresholdValue() is the normalized score (0-1) contributed when triggered.
 */
@Component
public class LocationChangeRule implements RiskRule {

    @Override
    public RuleType getRuleType() {
        return RuleType.LOCATION_CHANGE;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule) {
        String location = transaction.getLocation();

        if (location == null || location.isBlank()) {
            return new RuleResult(getRuleType().name(), false, 0.0,
                    "Location data unavailable - rule skipped safely, no risk contribution");
        }

        boolean known = profile.getKnownLocations().contains(location);
        if (known) {
            return new RuleResult(getRuleType().name(), false, 0.0, "Recognized location: " + location);
        }

        return new RuleResult(getRuleType().name(), true, rule.getThresholdValue().doubleValue(), "Unrecognized location: " + location);
    }
}
