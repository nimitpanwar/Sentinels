package com.example.entity;

import com.example.enums.CaseStatus;
import com.example.enums.Severity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the 'cases' table - the investigation unit an analyst
 * works. Multiple Alert rows for the same account within the merge/cooldown
 * window are grouped under one open Case (see AlertManager).
 */
@Entity
@Table(
    name = "cases",
    indexes = {
        @Index(name = "idx_case_account_status", columnList = "account_id, status")
    }
)
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Integer caseId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Optimistic lock: guards against lost updates when two lifecycle
    // actions (acknowledge/escalate/close) race on the same case (see
    // Appendix E "Concurrent Operations" test scenario) - the losing save
    // throws ObjectOptimisticLockingFailureException instead of silently
    // overwriting the other operator's change.
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public Case() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
