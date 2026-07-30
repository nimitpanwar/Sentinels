package com.example.riskengine.alert;

import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.entity.Transaction;
import com.example.enums.CaseStatus;
import com.example.repository.AlertRepository;
import com.example.repository.CaseRepository;
import com.example.riskengine.config.AlertConfig;
import com.example.riskengine.model.RiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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

    private final AlertConfig alertConfig;
    private final AlertRepository alertRepository;
    private final CaseRepository caseRepository;

    public AlertManager(AlertConfig alertConfig, AlertRepository alertRepository, CaseRepository caseRepository) {
        this.alertConfig = alertConfig;
        this.alertRepository = alertRepository;
        this.caseRepository = caseRepository;
    }

    /**
     * @return the created Alert, or empty if the risk score did not warrant one.
     */
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
        List<Case> candidates = caseRepository.findByAccountAccountIdAndStatusNotOrderByCreatedAtDesc(
                transaction.getAccountId(), CaseStatus.CLOSED);

        Optional<Case> existing = candidates.stream()
                .filter(c -> isWithinMergeWindow(c, riskResult))
                .findFirst();

        if (existing.isPresent()) {
            Case aCase = existing.get();
            int mergedScore = Math.max(aCase.getRiskScore().intValue(), riskResult.getRiskScore());
            aCase.setRiskScore(toScoreDecimal(mergedScore));
            aCase.setSeverity(alertConfig.severityFor(mergedScore));
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
        aCase = caseRepository.save(aCase);
        log.info("Created new case {} for account {} (score={})",
                aCase.getCaseId(), transaction.getAccountId(), riskResult.getRiskScore());
        return aCase;
    }

    /** Merge cooldown is measured from the case's most recent alert (or its creation time if it has none yet). */
    private boolean isWithinMergeWindow(Case aCase, RiskResult riskResult) {
        LocalDateTime referenceTime = alertRepository.findFirstByACaseCaseIdOrderByCreatedAtDesc(aCase.getCaseId())
                .map(Alert::getCreatedAt)
                .orElse(aCase.getCreatedAt());
        long minutesSince = ChronoUnit.MINUTES.between(referenceTime, riskResult.getTransactionTimestamp());
        return minutesSince <= alertConfig.getMergeCooldownMinutes();
    }

    private BigDecimal toScoreDecimal(int score) {
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    // ---- Case lifecycle actions (used by CaseController) ----

    public Optional<Case> acknowledge(Integer caseId) {
        return updateCaseStatus(caseId, CaseStatus.IN_REVIEW, null);
    }

    public Optional<Case> escalate(Integer caseId) {
        return updateCaseStatus(caseId, CaseStatus.ESCALATED, null);
    }

    public Optional<Case> close(Integer caseId, String resolutionNotes) {
        return updateCaseStatus(caseId, CaseStatus.CLOSED, resolutionNotes);
    }

    private Optional<Case> updateCaseStatus(Integer caseId, CaseStatus status, String resolutionNotes) {
        return caseRepository.findById(caseId).map(c -> {
            c.setStatus(status);
            if (resolutionNotes != null) {
                c.setResolutionNotes(resolutionNotes);
            }
            if (status == CaseStatus.CLOSED) {
                c.setClosedAt(LocalDateTime.now());
            }
            return caseRepository.save(c);
        });
    }
}

