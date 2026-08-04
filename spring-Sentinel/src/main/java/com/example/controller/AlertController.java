package com.example.controller;

import com.example.entity.Alert;
import com.example.entity.RuleEvaluation;
import com.example.enums.CaseStatus;
import com.example.repository.AlertRepository;
import com.example.repository.RuleEvaluationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only view of alerts produced by the Risk Engine + Alert Manager.
 * Transaction creation (POST /api/transactions) already returns the risk
 * score/alert result synchronously - these endpoints are for browsing/
 * re-checking alert state afterward (e.g. from a dashboard).
 *
 * NOTE: Lombok (@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;

    public AlertController(AlertRepository alertRepository, RuleEvaluationRepository ruleEvaluationRepository) {
        this.alertRepository = alertRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAll() {
        return ResponseEntity.ok(alertRepository.findAll().stream().toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getById(@PathVariable Integer id) {
        return alertRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns the rule evaluation audit rows for the transaction that triggered this alert.
     * Each row represents one rule that was evaluated (triggered or not), with its score and reason.
     */
    @GetMapping("/{id}/evaluations")
    public ResponseEntity<List<RuleEvaluation>> getEvaluations(@PathVariable Integer id) {
        return alertRepository.findById(id).map(alert -> {
            Integer txId = alert.getTransaction().getTransactionId();
            return ResponseEntity.ok(ruleEvaluationRepository.findByTransactionTransactionId(txId));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Transitions an alert through its lifecycle.
     * Allowed moves (Phase 1):
     *   OPEN         → ACKNOWLEDGED
     *   ACKNOWLEDGED → INVESTIGATING | DISMISSED
     * Body: { "status": "ACKNOWLEDGED", "resolutionNotes": "optional" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                          @RequestBody Map<String, String> body) {
        return alertRepository.findById(id).map(alert -> {
            String rawStatus = body.get("status");
            if (rawStatus == null) {
                return ResponseEntity.badRequest().body("Missing 'status' field");
            }

            CaseStatus target;
            try {
                target = CaseStatus.valueOf(rawStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Unknown status: " + rawStatus);
            }

            if (!isTransitionAllowed(alert.getStatus(), target)) {
                return ResponseEntity.badRequest()
                        .body("Transition from " + alert.getStatus() + " to " + target + " is not allowed");
            }

            alert.setStatus(target);
            String notes = body.get("resolutionNotes");

            if (target == CaseStatus.ACKNOWLEDGED) {
                alert.setAcknowledgedAt(LocalDateTime.now());
            } else if (target == CaseStatus.DISMISSED) {
                alert.setClosedAt(LocalDateTime.now());
                if (notes != null && !notes.isBlank()) alert.setResolutionNotes(notes);
            } else if (target == CaseStatus.INVESTIGATING) {
                // acknowledgedAt should already be set — no extra timestamp needed for now
                if (notes != null && !notes.isBlank()) alert.setResolutionNotes(notes);
            }

            return ResponseEntity.ok(alertRepository.save(alert));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Valid lifecycle transitions. */
    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED = Map.of(
            CaseStatus.OPEN,         Set.of(CaseStatus.ACKNOWLEDGED),
            CaseStatus.ACKNOWLEDGED, Set.of(CaseStatus.INVESTIGATING, CaseStatus.DISMISSED)
    );

    private boolean isTransitionAllowed(CaseStatus from, CaseStatus to) {
        Set<CaseStatus> allowed = ALLOWED.get(from);
        return allowed != null && allowed.contains(to);
    }
}

