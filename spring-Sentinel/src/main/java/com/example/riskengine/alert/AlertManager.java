package com.example.riskengine.alert;

import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.entity.Transaction;
import com.example.enums.CaseStatus;
import com.example.enums.InvestigationMessageStatus;
import com.example.enums.ResolutionReasonCode;
import com.example.enums.Severity;
import com.example.repository.AlertRepository;
import com.example.repository.CaseRepository;
import com.example.repository.InvestigationMessageRepository;
import com.example.riskengine.config.AlertConfig;
import com.example.riskengine.model.RiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Alert Manager.
 *
 * Responsibility (Separation of Risk Engine and Alert Management): takes
 * the Risk Engine's output and decides what action to take - create a new
 * Alert (and its Case), merge into an existing open Case for the same
 * account, or ignore the transaction if no alert is warranted.
 *
 * Adapted from backend/'s com.frauddetection.alert.AlertManager for the
 * real relational schema: a Case is the investigation unit an analyst
 * works (grouping recent alerts for an account); an Alert is always
 * created 1-per-triggering-transaction and links to its Case.
 */
@Service
public class AlertManager {

    private static final Logger log = LoggerFactory.getLogger(AlertManager.class);

    /** A case in either of these statuses is resolved and must never receive newly-merged alerts. */
    private static final Set<CaseStatus> TERMINAL_STATUSES = Set.of(CaseStatus.CLOSED, CaseStatus.DISMISSED);

    private final AlertConfig alertConfig;
    private final AlertRepository alertRepository;
    private final CaseRepository caseRepository;
    private final InvestigationMessageRepository investigationMessageRepository;

    public AlertManager(AlertConfig alertConfig,
                        AlertRepository alertRepository,
                        CaseRepository caseRepository,
                        InvestigationMessageRepository investigationMessageRepository) {
        this.alertConfig = alertConfig;
        this.alertRepository = alertRepository;
        this.caseRepository = caseRepository;
        this.investigationMessageRepository = investigationMessageRepository;
    }

    /**
     * @return the created Alert, or empty if the risk score did not warrant one.
     *
     * READ_COMMITTED: paired with the PESSIMISTIC_WRITE lock in
     * findOrCreateCase, this serializes concurrent evaluations for the same
     * account around the case lookup/create-or-merge step, and avoids the
     * wider gap-locking that MySQL's default REPEATABLE_READ would take for
     * a locked range scan under concurrent inserts (higher deadlock risk).
     *
     * NOTE: an in-JVM ReentrantLock replacement was tried here and reverted -
     * releasing a Java lock inside this method body happens BEFORE Spring's
     * @Transactional proxy actually commits (commit happens in the proxy,
     * after this method returns), so a second thread could acquire the JVM
     * lock and read stale (not-yet-committed) case state. Confirmed via a
     * live test: two concurrent evaluations for the same account both saw
     * "no open case" and created duplicate cases 31ms apart. A DB row lock
     * doesn't have this gap since it's held until the actual commit.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<Alert> process(RiskResult riskResult, Transaction transaction) {
        // Step 1: Check alert threshold.
        if (riskResult.getRiskScore() < alertConfig.getMinScoreToCreateAlert()) {
            log.info("Transaction {} scored {} - below threshold, no alert created",
                    riskResult.getTransactionId(), riskResult.getRiskScore());
            return Optional.empty();
        }

        // Step 2: Find an existing open case for this account within the merge window, or create one.
        Case aCase = findOrCreateCase(transaction, riskResult);

        // Step 3: Always create one Alert row for this triggering transaction, linked to the case.
        Alert alert = new Alert();
        alert.setTransaction(transaction);
        alert.setCase(aCase);
        alert.setRiskScore(toScoreDecimal(riskResult.getRiskScore()));
        alert.setSeverity(alertConfig.severityFor(riskResult.getRiskScore()));
        alert.setStatus(CaseStatus.OPEN);
        alert = alertRepository.save(alert);

        log.info("Created alert {} for transaction {} linked to case {} (score={})",
                alert.getAlertId(), riskResult.getTransactionId(), aCase.getCaseId(), riskResult.getRiskScore());

        return Optional.of(alert);
    }

    private Case findOrCreateCase(Transaction transaction, RiskResult riskResult) {
        // Pessimistic write lock: held until this transaction commits, so a
        // second concurrent evaluation for the same account blocks here
        // instead of also seeing "no open case" and creating a duplicate one.
        List<Case> candidates;
        try {
            candidates = caseRepository.findByAccountForUpdate(
                    transaction.getAccountId(), TERMINAL_STATUSES);
        } catch (IllegalArgumentException ex) {
            // A case row for this account has a status value that no longer exists in
            // CaseStatus (e.g. left over from an old enum definition, migrated data, or a
            // manual DB edit). Don't let one corrupt row silently block every future alert
            // for this account - log it clearly and fall back to opening a new case instead
            // of merging into the unreadable one.
            log.error("Could not load existing cases for account {} - a row has a status value " +
                            "not in CaseStatus (stale/invalid data). Opening a new case instead of merging. Cause: {}",
                    transaction.getAccountId(), ex.getMessage());
            candidates = List.of();
        }

        Optional<Case> existing = candidates.stream()
                .filter(c -> isWithinMergeWindow(c, riskResult))
                .findFirst();

        if (existing.isPresent()) {
            Case aCase = existing.get();
            int mergedScore = Math.max(aCase.getRiskScore().intValue(), riskResult.getRiskScore());
            aCase.setRiskScore(toScoreDecimal(mergedScore));
            aCase.setSeverity(alertConfig.severityFor(mergedScore));
            aCase.setLastAlertAt(riskResult.getTransactionTimestamp());
            caseRepository.save(aCase);
            log.info("Merged transaction {} into existing case {} (new score={})",
                    riskResult.getTransactionId(), aCase.getCaseId(), mergedScore);
            return aCase;
        }

        Case aCase = new Case();
        aCase.setAccount(transaction.getAccount());
        aCase.setRiskScore(toScoreDecimal(riskResult.getRiskScore()));
        aCase.setSeverity(alertConfig.severityFor(riskResult.getRiskScore()));
        aCase.setStatus(CaseStatus.OPEN);
        aCase.setLastAlertAt(riskResult.getTransactionTimestamp());
        aCase = caseRepository.save(aCase);
        log.info("Created new case {} for account {} (score={})",
                aCase.getCaseId(), transaction.getAccountId(), riskResult.getRiskScore());
        return aCase;
    }

    /** Merge cooldown is measured from the case's most recent alert (or its creation time if it has none yet). */
    private boolean isWithinMergeWindow(Case aCase, RiskResult riskResult) {
        LocalDateTime referenceTime = aCase.getLastAlertAt() != null ? aCase.getLastAlertAt() : aCase.getCreatedAt();
        long minutesSince = ChronoUnit.MINUTES.between(referenceTime, riskResult.getTransactionTimestamp());
        return minutesSince <= alertConfig.getMergeCooldownMinutes();
    }

    private BigDecimal toScoreDecimal(int score) {
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    // ---- Case lifecycle actions (used by CaseController) ----
    //
    // Enforced state machine (see Appendix E "Alert Status Validation" -
    // closing without acknowledging first, or reopening a closed/dismissed
    // case, are both deliberately rejected rather than silently allowed):
    //
    //   OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED
    //     |            |
    //     v            v
    // INVESTIGATING  DISMISSED
    //              |                 |
    //              v                 v
    //          DISMISSED         DISMISSED
    //
    // CLOSED and DISMISSED are terminal - no outgoing transitions.

    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED_TRANSITIONS = Map.of(
            // OPEN -> INVESTIGATING is allowed for one-click analyst takeover
            // from the alert detail page's Investigate action.
            // OPEN -> DISMISSED is also allowed for immediate false-positive handling.
            // OPEN -> ESCALATED is allowed for immediate escalation from investigation actions.
            CaseStatus.OPEN, Set.of(CaseStatus.ACKNOWLEDGED, CaseStatus.INVESTIGATING, CaseStatus.DISMISSED, CaseStatus.ESCALATED),
            CaseStatus.ACKNOWLEDGED, Set.of(CaseStatus.INVESTIGATING, CaseStatus.ESCALATED, CaseStatus.CLOSED, CaseStatus.DISMISSED),
            CaseStatus.INVESTIGATING, Set.of(CaseStatus.ESCALATED, CaseStatus.CLOSED, CaseStatus.DISMISSED),
            CaseStatus.ESCALATED, Set.of(CaseStatus.CLOSED, CaseStatus.DISMISSED)
            // CLOSED, DISMISSED: intentionally absent - terminal states.
    );

    @Transactional
    public Optional<Case> acknowledge(Integer caseId) {
        return updateCaseStatus(caseId, CaseStatus.ACKNOWLEDGED, null, null, null);
    }

    /** "Mark as Investigating" - moves an already-acknowledged case into active investigation. */
    @Transactional
    public Optional<Case> investigate(Integer caseId) {
        return updateCaseStatus(caseId, CaseStatus.INVESTIGATING, null, null, null);
    }

    @Transactional
    public Optional<Case> close(Integer caseId, String resolutionNotes, ResolutionReasonCode reasonCode) {
        return updateCaseStatus(caseId, CaseStatus.CLOSED, resolutionNotes, reasonCode, null);
    }

    /** Marks the case a false positive / not requiring action - a terminal state distinct from CLOSED. */
    @Transactional
    public Optional<Case> dismiss(Integer caseId, String resolutionNotes, ResolutionReasonCode reasonCode) {
        return updateCaseStatus(caseId, CaseStatus.DISMISSED, resolutionNotes, reasonCode, null);
    }

    /**
     * Dismiss variant for alert-scoped workflows where gating must follow
     * the selected alert severity instead of merged case severity.
     */
    @Transactional
    public Optional<Case> dismissWithSeverity(Integer caseId,
                                              String resolutionNotes,
                                              ResolutionReasonCode reasonCode,
                                              Severity gatingSeverity) {
        return updateCaseStatus(caseId, CaseStatus.DISMISSED, resolutionNotes, reasonCode, gatingSeverity);
    }

    /** Escalates the case for deeper/manual review while keeping it active. */
    @Transactional
    public Optional<Case> escalate(Integer caseId, String resolutionNotes, ResolutionReasonCode reasonCode) {
        return updateCaseStatus(caseId, CaseStatus.ESCALATED, resolutionNotes, reasonCode, null);
    }

    @Transactional
    public Case saveCase(Case aCase) {
        return caseRepository.save(aCase);
    }

    @Transactional(readOnly = true)
    public Optional<Case> findCase(Integer caseId) {
        return caseRepository.findById(caseId);
    }

    private Optional<Case> updateCaseStatus(Integer caseId,
                                            CaseStatus newStatus,
                                            String resolutionNotes,
                                            ResolutionReasonCode reasonCode,
                                            Severity gatingSeverityOverride) {
        return caseRepository.findById(caseId).map(c -> {
            CaseStatus currentStatus = c.getStatus();
            if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
                throw new InvalidCaseTransitionException(currentStatus, newStatus);
            }

            if (newStatus == CaseStatus.CLOSED || newStatus == CaseStatus.DISMISSED) {
                enforceSeverityGates(c, newStatus, gatingSeverityOverride);
            }

            c.setStatus(newStatus);
            if (resolutionNotes != null) {
                c.setResolutionNotes(resolutionNotes);
            }
            if (reasonCode != null) {
                c.setResolutionReasonCode(reasonCode);
            }
            if (newStatus == CaseStatus.ACKNOWLEDGED && c.getAcknowledgedAt() == null) {
                c.setAcknowledgedAt(LocalDateTime.now(ZoneOffset.UTC));
            }
            if (newStatus == CaseStatus.CLOSED || newStatus == CaseStatus.DISMISSED) {
                c.setClosedAt(LocalDateTime.now(ZoneOffset.UTC));
            }
            Case saved = caseRepository.save(c);

            // Keep every Alert row linked to this case in sync with its lifecycle -
            // otherwise Alert.status would stay frozen at OPEN forever (see
            // entity/Alert.java), even though the case it belongs to has moved on.
            alertRepository.updateStatusByCaseId(saved.getCaseId(), newStatus);

            return saved;
        });
    }

    private void enforceSeverityGates(Case aCase, CaseStatus targetStatus, Severity gatingSeverityOverride) {
        Severity severity = gatingSeverityOverride != null
                ? gatingSeverityOverride
                : (aCase.getSeverity() == null ? Severity.LOW : aCase.getSeverity());

        if (isBlank(aCase.getInvestigationAnalystNote())) {
            throw new IllegalStateException("Add analyst note before setting case to " + targetStatus);
        }

        if (severity == Severity.MID || severity == Severity.HIGH) {
            boolean outreachSent = investigationMessageRepository.existsByACaseCaseIdAndDeliveryStatus(
                    aCase.getCaseId(), InvestigationMessageStatus.SENT);
            if (!outreachSent) {
                throw new IllegalStateException("Send outreach before setting " + severity + " case to " + targetStatus);
            }
            if (!Boolean.TRUE.equals(aCase.getInvestigationChecklistComplete())) {
                throw new IllegalStateException("Complete investigation checklist before setting " + severity + " case to " + targetStatus);
            }
        }

        if (severity == Severity.HIGH) {
            if (isBlank(aCase.getHighRiskJustification())
                    || aCase.getHighRiskAcknowledgedAt() == null
                    || aCase.getHighRiskSecondConfirmAt() == null) {
                throw new IllegalStateException("Complete high-risk self-approval before setting case to " + targetStatus);
            }
            if (aCase.getHighRiskCooldownUntil() == null || aCase.getHighRiskCooldownUntil().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                throw new IllegalStateException("Wait for high-risk cooldown to finish before setting case to " + targetStatus);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

