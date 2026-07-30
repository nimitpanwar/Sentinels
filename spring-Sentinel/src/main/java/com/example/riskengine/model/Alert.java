package com.example.riskengine.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A case/alert grouping one or more suspicious transactions for the same
 * account, as produced by the Alert Manager.
 * Ported from backend/'s com.frauddetection.model.Alert, unchanged.
 */
public class Alert {

    private final String id;
    private final String accountId;
    private final String payeeId;
    private final Set<String> transactionIds = new LinkedHashSet<>();
    private int riskScore; // highest risk score seen across merged transactions
    private AlertSeverity severity;
    private final Set<String> triggeredRules = new LinkedHashSet<>();
    private final Set<String> evidence = new LinkedHashSet<>();
    private AlertStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String resolutionNotes;

    public Alert(String id, String accountId, String payeeId, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.payeeId = payeeId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = AlertStatus.OPEN;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getPayeeId() { return payeeId; }
    public Set<String> getTransactionIds() { return transactionIds; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public Set<String> getTriggeredRules() { return triggeredRules; }
    public Set<String> getEvidence() { return evidence; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    /** True while the alert is still active and eligible for merging. */
    public boolean isActive() {
        return status == AlertStatus.OPEN || status == AlertStatus.ACKNOWLEDGED || status == AlertStatus.INVESTIGATING;
    }

    @Override
    public String toString() {
        return String.format("Alert[id=%s, account=%s, status=%s, severity=%s, riskScore=%d, txs=%s, rules=%s]",
                id, accountId, status, severity, riskScore, transactionIds, triggeredRules);
    }
}
