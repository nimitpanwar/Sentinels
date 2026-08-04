package com.example.controller;

import com.example.dto.InvestigationAnalystActionRequest;
import com.example.dto.HighRiskSelfApprovalRequest;
import com.example.dto.InvestigationMessageResponse;
import com.example.dto.InvestigationProfileResponse;
import com.example.dto.InvestigationProfileUpdateRequest;
import com.example.dto.InvestigationSendRequest;
import com.example.dto.InvestigationSendResponse;
import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.entity.RuleEvaluation;
import com.example.enums.CaseStatus;
import com.example.repository.AlertRepository;
import com.example.repository.RuleEvaluationRepository;
import com.example.riskengine.alert.AlertManager;
import com.example.riskengine.alert.InvalidCaseTransitionException;
import com.example.service.InvestigationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
    private final InvestigationService investigationService;
    private final AlertManager alertManager;

    public AlertController(AlertRepository alertRepository,
                           RuleEvaluationRepository ruleEvaluationRepository,
                           InvestigationService investigationService,
                           AlertManager alertManager) {
        this.alertRepository = alertRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.investigationService = investigationService;
        this.alertManager = alertManager;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAll() {
        return ResponseEntity.ok(alertRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
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

            String scope = body.getOrDefault("updateScope", "ALERT");
            CaseStatus target;
            try {
                target = CaseStatus.valueOf(rawStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Unknown status: " + rawStatus);
            }

            if ("CASE".equalsIgnoreCase(scope)) {
                Case aCase = alert.getCase();
                if (aCase == null) {
                    return ResponseEntity.badRequest().body("Cannot apply CASE scope: alert has no case");
                }
                String notes = body.get("resolutionNotes");
                applyCaseScopedTransition(aCase.getCaseId(), target, notes);
                return alertRepository.findById(id)
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build());
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

    private void applyCaseScopedTransition(Integer caseId, CaseStatus target, String notes) {
    switch (target) {
        case ACKNOWLEDGED -> alertManager.acknowledge(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        case INVESTIGATING -> alertManager.investigate(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        case DISMISSED -> alertManager.dismiss(caseId, notes, null)
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        case CLOSED -> alertManager.close(caseId, notes, null)
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        default -> throw new IllegalArgumentException("Unsupported CASE scope target status: " + target);
    }
    }

    /** Valid lifecycle transitions. */
    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED = Map.of(
            CaseStatus.OPEN,         Set.of(CaseStatus.ACKNOWLEDGED, CaseStatus.INVESTIGATING),
            CaseStatus.ACKNOWLEDGED, Set.of(CaseStatus.INVESTIGATING, CaseStatus.DISMISSED),
            CaseStatus.INVESTIGATING, Set.of(CaseStatus.DISMISSED)
    );

    private boolean isTransitionAllowed(CaseStatus from, CaseStatus to) {
        Set<CaseStatus> allowed = ALLOWED.get(from);
        return allowed != null && allowed.contains(to);
    }

    /** Sends an analyst-crafted customer outreach email for this alert investigation. */
    @PostMapping("/{id}/investigation/send")
    public ResponseEntity<InvestigationSendResponse> sendInvestigationMessage(@PathVariable Integer id,
                                                                              @RequestBody InvestigationSendRequest request) {
        return ResponseEntity.ok(investigationService.send(id, request));
    }

    /** Marks an alert/case as actively investigating from the detail page action. */
    @PatchMapping("/{id}/investigation/start")
    public ResponseEntity<Alert> startInvestigation(@PathVariable Integer id) {
        return ResponseEntity.ok(investigationService.markInvestigating(id));
    }

    /** Returns recorded outreach thread items for this alert, newest first. */
    @GetMapping("/{id}/investigation/thread")
    public ResponseEntity<List<InvestigationMessageResponse>> getInvestigationThread(@PathVariable Integer id) {
        return ResponseEntity.ok(investigationService.getThread(id));
    }

    /** Returns severity-based investigation requirements and completion status for this alert's case. */
    @GetMapping("/{id}/investigation/profile")
    public ResponseEntity<InvestigationProfileResponse> getInvestigationProfile(@PathVariable Integer id) {
        return ResponseEntity.ok(investigationService.getInvestigationProfile(id));
    }

    /** Updates analyst note/checklist progress used by severity gates. */
    @PatchMapping("/{id}/investigation/profile")
    public ResponseEntity<InvestigationProfileResponse> updateInvestigationProfile(@PathVariable Integer id,
                                                                                   @RequestBody InvestigationProfileUpdateRequest request) {
        return ResponseEntity.ok(investigationService.updateInvestigationProfile(id, request));
    }

    /** Completes high-risk self-approval and starts cooldown window for HIGH severity final actions. */
    @PostMapping("/{id}/investigation/high-risk-self-approval")
    public ResponseEntity<InvestigationProfileResponse> submitHighRiskSelfApproval(@PathVariable Integer id,
                                                                                   @RequestBody HighRiskSelfApprovalRequest request) {
        return ResponseEntity.ok(investigationService.submitHighRiskSelfApproval(id, request));
    }

    /** Applies analyst action after reviewing customer response in investigation workflow. */
    @PatchMapping("/{id}/investigation/action")
    public ResponseEntity<InvestigationSendResponse> applyInvestigationAction(@PathVariable Integer id,
                                                                              @RequestBody InvestigationAnalystActionRequest request) {
        return ResponseEntity.ok(investigationService.applyAnalystAction(id, request));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleInvestigationRequestErrors(RuntimeException ex) {
        HttpStatus status = ex instanceof IllegalStateException ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCaseTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(InvalidCaseTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}

