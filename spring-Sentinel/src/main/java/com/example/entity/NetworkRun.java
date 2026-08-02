package com.example.entity;

import com.example.enums.NetworkRunStatus;
import com.example.enums.NetworkRunTrigger;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * JPA entity for the 'network_runs' table - one row per execution of the
 * (Python/NetworkX) network-analysis batch job. Written by the Python job
 * itself (plain SQL, not through this JPA mapping) and read-only from the
 * Spring side via NetworkRunRepository - this entity exists purely so the
 * REST API can report run history/freshness to the operator.
 *
 * Kept append-only/immutable in spirit: a run's row is only ever updated by
 * the SAME run (RUNNING -> COMPLETED/FAILED), never retroactively edited.
 */
@Entity
@Table(name = "network_runs")
public class NetworkRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Integer runId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NetworkRunStatus status = NetworkRunStatus.RUNNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 15)
    private NetworkRunTrigger triggerType = NetworkRunTrigger.SCHEDULED;

    @Column(name = "lookback_days", nullable = false)
    private Integer lookbackDays;

    // Free-form version tag written by the Python job (e.g. "1.0.0") so a
    // future algorithm change is traceable against historical scores -
    // lets you answer "was this account's score computed with the old or
    // new weighting?" instead of guessing from computed_at alone.
    @Column(name = "algorithm_version", nullable = false, length = 20)
    private String algorithmVersion;

    @Column(name = "accounts_analyzed")
    private Integer accountsAnalyzed;

    @Column(name = "accounts_flagged")
    private Integer accountsFlagged;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public NetworkRun() {
    }

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Integer getRunId() { return runId; }
    public void setRunId(Integer runId) { this.runId = runId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public NetworkRunStatus getStatus() { return status; }
    public void setStatus(NetworkRunStatus status) { this.status = status; }
    public NetworkRunTrigger getTriggerType() { return triggerType; }
    public void setTriggerType(NetworkRunTrigger triggerType) { this.triggerType = triggerType; }
    public Integer getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(Integer lookbackDays) { this.lookbackDays = lookbackDays; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public Integer getAccountsAnalyzed() { return accountsAnalyzed; }
    public void setAccountsAnalyzed(Integer accountsAnalyzed) { this.accountsAnalyzed = accountsAnalyzed; }
    public Integer getAccountsFlagged() { return accountsFlagged; }
    public void setAccountsFlagged(Integer accountsFlagged) { this.accountsFlagged = accountsFlagged; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
