package com.example.riskengine.repository;

import com.example.riskengine.model.AlertSeverity;
import com.example.riskengine.model.AlertStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * DEAD CODE: superseded by com.example.entity.Alert (the real JPA entity for
 * the schema's 'alerts' table). No longer annotated @Entity so Hibernate
 * does not attempt to manage/create a table for it (which would otherwise
 * collide with entity.Alert's mapping of the same 'alerts' table name).
 *
 * NOTE: Lombok (@Data/@Builder/etc.) intentionally not used - see entity/Transaction.java note.
 */
public class AlertEntity {

    @Id
    @Column(length = 30)
    private String id;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "payee_id", length = 50)
    private String payeeId;

    @ElementCollection
    @CollectionTable(name = "alert_transaction_ids", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "transaction_id")
    private Set<String> transactionIds = new LinkedHashSet<>();

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AlertSeverity severity;

    @ElementCollection
    @CollectionTable(name = "alert_triggered_rules", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "rule_name", length = 50)
    private Set<String> triggeredRules = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "alert_evidence", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "evidence_text", length = 500)
    private Set<String> evidence = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    public AlertEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPayeeId() { return payeeId; }
    public void setPayeeId(String payeeId) { this.payeeId = payeeId; }
    public Set<String> getTransactionIds() { return transactionIds; }
    public void setTransactionIds(Set<String> transactionIds) { this.transactionIds = transactionIds; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public Set<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(Set<String> triggeredRules) { this.triggeredRules = triggeredRules; }
    public Set<String> getEvidence() { return evidence; }
    public void setEvidence(Set<String> evidence) { this.evidence = evidence; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written stand-in for Lombok's @Builder (see class-level note). */
    public static class Builder {
        private final AlertEntity entity = new AlertEntity();

        public Builder id(String id) { entity.id = id; return this; }
        public Builder accountId(String accountId) { entity.accountId = accountId; return this; }
        public Builder payeeId(String payeeId) { entity.payeeId = payeeId; return this; }
        public Builder transactionIds(Set<String> transactionIds) { entity.transactionIds = new LinkedHashSet<>(transactionIds); return this; }
        public Builder riskScore(int riskScore) { entity.riskScore = riskScore; return this; }
        public Builder severity(AlertSeverity severity) { entity.severity = severity; return this; }
        public Builder triggeredRules(Set<String> triggeredRules) { entity.triggeredRules = new LinkedHashSet<>(triggeredRules); return this; }
        public Builder evidence(Set<String> evidence) { entity.evidence = new LinkedHashSet<>(evidence); return this; }
        public Builder status(AlertStatus status) { entity.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { entity.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { entity.updatedAt = updatedAt; return this; }
        public Builder resolutionNotes(String resolutionNotes) { entity.resolutionNotes = resolutionNotes; return this; }

        public AlertEntity build() { return entity; }
    }
}

