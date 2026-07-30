/**
 * TransactionResponse DTO (Data Transfer Object)
 * 
 * PURPOSE: Defines the shape of data that the API sends back to callers
 *          after creating or retrieving a transaction.
 * 
 * FIELDS: Includes EVERYTHING—both what the caller sent plus system-generated fields:
 *   - transactionId: Database ID (Integer, real relational schema)
 *   - accountId, payeeId, amount, currency, type: From the request
 *   - transactionTimestamp: When it happened
 *   - status: COMPLETED/PENDING/FAILED
 *   - createdAt: When it was saved to database
 *   - riskScore/triggeredRules/evidence: Risk engine output
 *   - alertId/alertSeverity/alertStatus/caseId/caseSeverity/caseStatus: Alert manager output
 * 
 * WHY ALL FIELDS: The frontend needs to see everything about a transaction,
 *                 including the auto-generated fields and current status.
 * 
 * WHY SEPARATE: DTOs act as a contract between backend and frontend.
 *               The frontend gets only what's meant for display/use.
 */
package com.example.dto;

import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// NOTE: Lombok (@Data/@Builder) intentionally not used - see entity/Transaction.java note.
public class TransactionResponse {

    private Integer transactionId;
    private Integer accountId;
    private Integer payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private LocalDateTime transactionTimestamp;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
    private String location;
    private String merchantCategory;

    // ---- Risk engine / alert manager output, populated synchronously after evaluation ----
    private Integer riskScore;
    private List<String> triggeredRules;
    private List<String> evidence;
    private Integer alertId;
    private String alertSeverity;
    private String alertStatus;
    private Integer caseId;
    private String caseSeverity;
    private String caseStatus;

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }
    public Integer getPayeeId() { return payeeId; }
    public void setPayeeId(Integer payeeId) { this.payeeId = payeeId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public LocalDateTime getTransactionTimestamp() { return transactionTimestamp; }
    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) { this.transactionTimestamp = transactionTimestamp; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    public String getAlertSeverity() { return alertSeverity; }
    public void setAlertSeverity(String alertSeverity) { this.alertSeverity = alertSeverity; }
    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }
    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public String getCaseSeverity() { return caseSeverity; }
    public void setCaseSeverity(String caseSeverity) { this.caseSeverity = caseSeverity; }
    public String getCaseStatus() { return caseStatus; }
    public void setCaseStatus(String caseStatus) { this.caseStatus = caseStatus; }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written stand-in for Lombok's @Builder (see class-level note). */
    public static class Builder {
        private final TransactionResponse resp = new TransactionResponse();

        public Builder transactionId(Integer transactionId) { resp.transactionId = transactionId; return this; }
        public Builder accountId(Integer accountId) { resp.accountId = accountId; return this; }
        public Builder payeeId(Integer payeeId) { resp.payeeId = payeeId; return this; }
        public Builder amount(BigDecimal amount) { resp.amount = amount; return this; }
        public Builder currency(String currency) { resp.currency = currency; return this; }
        public Builder type(TransactionType type) { resp.type = type; return this; }
        public Builder transactionTimestamp(LocalDateTime transactionTimestamp) { resp.transactionTimestamp = transactionTimestamp; return this; }
        public Builder status(TransactionStatus status) { resp.status = status; return this; }
        public Builder description(String description) { resp.description = description; return this; }
        public Builder createdAt(LocalDateTime createdAt) { resp.createdAt = createdAt; return this; }
        public Builder location(String location) { resp.location = location; return this; }
        public Builder merchantCategory(String merchantCategory) { resp.merchantCategory = merchantCategory; return this; }
        public Builder riskScore(Integer riskScore) { resp.riskScore = riskScore; return this; }
        public Builder triggeredRules(List<String> triggeredRules) { resp.triggeredRules = triggeredRules; return this; }
        public Builder evidence(List<String> evidence) { resp.evidence = evidence; return this; }
        public Builder alertId(Integer alertId) { resp.alertId = alertId; return this; }
        public Builder alertSeverity(String alertSeverity) { resp.alertSeverity = alertSeverity; return this; }
        public Builder alertStatus(String alertStatus) { resp.alertStatus = alertStatus; return this; }
        public Builder caseId(Integer caseId) { resp.caseId = caseId; return this; }
        public Builder caseSeverity(String caseSeverity) { resp.caseSeverity = caseSeverity; return this; }
        public Builder caseStatus(String caseStatus) { resp.caseStatus = caseStatus; return this; }

        public TransactionResponse build() { return resp; }
    }
}

