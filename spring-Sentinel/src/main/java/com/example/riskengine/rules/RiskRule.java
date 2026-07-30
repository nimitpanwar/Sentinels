package com.example.riskengine.rules;

import com.example.entity.Rule;
import com.example.entity.Transaction;
import com.example.enums.RuleType;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RuleResult;

/**
 * Contract for a single individual risk rule. Each rule is independent and
 * unaware of the others - new rules can be added by simply implementing this
 * interface and registering as a Spring bean (auto-discovered by RiskEngine).
 * Ported from backend/'s com.frauddetection.rules.RiskRule - now operates
 * directly on the JPA Transaction entity, and reads its weight/threshold/
 * lookback config from the DB-backed Rule row instead of a hardcoded config
 * class (see entity/Rule.java for what each threshold_value means per type).
 */
public interface RiskRule {

    /** Which row in the 'rules' table this Java implementation services. */
    RuleType getRuleType();

    /** Evaluate the transaction against this rule and return a 0-1 score. */
    RuleResult evaluate(Transaction transaction, HistoricalProfile profile, Rule rule);
}

