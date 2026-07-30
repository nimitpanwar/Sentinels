package com.example.riskengine.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full output of the Risk Engine for a single transaction.
 * This is the object handed off to the Alert Manager - the Risk Engine
 * itself never decides whether to create/update/ignore an alert
 * (Separation of Risk Engine and Alert Management).
 * Ported from backend/'s com.frauddetection.model.RiskResult - IDs are
 * Integer to match the real relational schema (transaction_id/account_id/payee_id).
 */
public class RiskResult {

    private final Integer transactionId;
    private final Integer accountId;
    private final Integer payeeId;
    private final int riskScore; // 0 - 100
    private final List<RuleResult> ruleResults;
    private final LocalDateTime transactionTimestamp;

    public RiskResult(Integer transactionId, Integer accountId, Integer payeeId, int riskScore,
                       List<RuleResult> ruleResults, LocalDateTime transactionTimestamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.payeeId = payeeId;
        this.riskScore = riskScore;
        this.ruleResults = ruleResults;
        this.transactionTimestamp = transactionTimestamp;
    }

    public Integer getTransactionId() { return transactionId; }
    public Integer getAccountId() { return accountId; }
    public Integer getPayeeId() { return payeeId; }
    public int getRiskScore() { return riskScore; }
    public List<RuleResult> getRuleResults() { return ruleResults; }
    public LocalDateTime getTransactionTimestamp() { return transactionTimestamp; }

    public List<RuleResult> getTriggeredRules() {
        return ruleResults.stream().filter(RuleResult::isTriggered).toList();
    }
}
