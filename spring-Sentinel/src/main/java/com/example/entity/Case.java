package com.example.entity;

import com.example.enums.CaseStatus;
import com.example.enums.ResolutionReasonCode;
import com.example.enums.Severity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    // Set the first time this case is acknowledged (see AlertManager.updateCaseStatus).
    // Used to compute "average time to acknowledge" for the stats endpoint.
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "investigation_analyst_note", columnDefinition = "TEXT")
    private String investigationAnalystNote;

    @Column(name = "investigation_analyst_note_at")
    private LocalDateTime investigationAnalystNoteAt;

    @Column(name = "investigation_checklist_complete", nullable = false)
    private Boolean investigationChecklistComplete = Boolean.FALSE;

    @Column(name = "investigation_checklist_completed_at")
    private LocalDateTime investigationChecklistCompletedAt;

    @Column(name = "high_risk_justification", columnDefinition = "TEXT")
    private String highRiskJustification;

    @Column(name = "high_risk_acknowledged_at")
    private LocalDateTime highRiskAcknowledgedAt;

    @Column(name = "high_risk_second_confirm_at")
    private LocalDateTime highRiskSecondConfirmAt;

    @Column(name = "high_risk_cooldown_until")
    private LocalDateTime highRiskCooldownUntil;

    // Structured counterpart to resolutionNotes, set only when status becomes
    // CLOSED/DISMISSED (see AlertManager.updateCaseStatus). Distinguishes
    // "confirmed fraud" from other resolutions without parsing free text -
    // consumed by the network-analysis job to seed personalized PageRank.
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_reason_code", length = 40)
    private ResolutionReasonCode resolutionReasonCode;

    // Denormalized copy of this case's most recent alert's createdAt, kept in
    // sync by AlertManager on every merge/create. Lets isWithinMergeWindow()
    // do a plain in-memory comparison instead of a separate DB query per
    // candidate case - safe because it's written in the SAME transaction/
    // commit as the rest of the case row (no atomicity gap introduced).
    @Column(name = "last_alert_at")
    private LocalDateTime lastAlertAt;

    public Case() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
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
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getInvestigationAnalystNote() { return investigationAnalystNote; }
    public void setInvestigationAnalystNote(String investigationAnalystNote) { this.investigationAnalystNote = investigationAnalystNote; }
    public LocalDateTime getInvestigationAnalystNoteAt() { return investigationAnalystNoteAt; }
    public void setInvestigationAnalystNoteAt(LocalDateTime investigationAnalystNoteAt) { this.investigationAnalystNoteAt = investigationAnalystNoteAt; }
    public Boolean getInvestigationChecklistComplete() { return investigationChecklistComplete; }
    public void setInvestigationChecklistComplete(Boolean investigationChecklistComplete) { this.investigationChecklistComplete = investigationChecklistComplete; }
    public LocalDateTime getInvestigationChecklistCompletedAt() { return investigationChecklistCompletedAt; }
    public void setInvestigationChecklistCompletedAt(LocalDateTime investigationChecklistCompletedAt) { this.investigationChecklistCompletedAt = investigationChecklistCompletedAt; }
    public String getHighRiskJustification() { return highRiskJustification; }
    public void setHighRiskJustification(String highRiskJustification) { this.highRiskJustification = highRiskJustification; }
    public LocalDateTime getHighRiskAcknowledgedAt() { return highRiskAcknowledgedAt; }
    public void setHighRiskAcknowledgedAt(LocalDateTime highRiskAcknowledgedAt) { this.highRiskAcknowledgedAt = highRiskAcknowledgedAt; }
    public LocalDateTime getHighRiskSecondConfirmAt() { return highRiskSecondConfirmAt; }
    public void setHighRiskSecondConfirmAt(LocalDateTime highRiskSecondConfirmAt) { this.highRiskSecondConfirmAt = highRiskSecondConfirmAt; }
    public LocalDateTime getHighRiskCooldownUntil() { return highRiskCooldownUntil; }
    public void setHighRiskCooldownUntil(LocalDateTime highRiskCooldownUntil) { this.highRiskCooldownUntil = highRiskCooldownUntil; }
    public ResolutionReasonCode getResolutionReasonCode() { return resolutionReasonCode; }
    public void setResolutionReasonCode(ResolutionReasonCode resolutionReasonCode) { this.resolutionReasonCode = resolutionReasonCode; }
    public LocalDateTime getLastAlertAt() { return lastAlertAt; }
    public void setLastAlertAt(LocalDateTime lastAlertAt) { this.lastAlertAt = lastAlertAt; }
}
