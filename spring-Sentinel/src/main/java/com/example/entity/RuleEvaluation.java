package com.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the 'rule_evaluations' table - an audit row logged for
 * EVERY active rule evaluated against a transaction (not just the ones that
 * triggered), so the full evaluation history is inspectable later.
 */
@Entity
@Table(
    name = "rule_evaluations",
    indexes = {
        @Index(name = "idx_ruleeval_transaction", columnList = "transaction_id")
    }
)
public class RuleEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Integer evaluationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Column(name = "risk_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal riskScore;

    @Column(nullable = false)
    private boolean triggered;

    @Column(length = 255)
    private String reason;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;

    public RuleEvaluation() {
    }

    @PrePersist
    public void prePersist() {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }

    public Integer getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Integer evaluationId) { this.evaluationId = evaluationId; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Rule getRule() { return rule; }
    public void setRule(Rule rule) { this.rule = rule; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
