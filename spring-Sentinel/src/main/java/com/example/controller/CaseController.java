package com.example.controller;

import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.repository.AlertRepository;
import com.example.dto.CaseStatsResponse;
import com.example.enums.CaseStatus;
import com.example.enums.ResolutionReasonCode;
import com.example.repository.CaseRepository;
import com.example.riskengine.alert.AlertManager;
import com.example.riskengine.alert.InvalidCaseTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read/lifecycle endpoints for cases (the investigation unit that groups
 * merged alerts for an account). Case creation itself is never done via
 * this controller - cases are only created automatically by AlertManager
 * when a transaction's risk score crosses the alert threshold.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepository;
    private final AlertManager alertManager;
    private final AlertRepository alertRepository;

    public CaseController(CaseRepository caseRepository, AlertManager alertManager, AlertRepository alertRepository) {
        this.caseRepository = caseRepository;
        this.alertManager = alertManager;
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public ResponseEntity<List<Case>> getAll() {
        return ResponseEntity.ok(caseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Case> getById(@PathVariable Integer id) {
        return caseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** All alerts that belong to this case, newest first. */
    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<Alert>> getAlerts(@PathVariable Integer id) {
        return ResponseEntity.ok(alertRepository.findByACaseCaseIdOrderByCreatedAtDesc(id));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Case> acknowledge(@PathVariable Integer id) {
        return alertManager.acknowledge(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** "Mark as Investigating" - moves an acknowledged case into active investigation. */
    @PatchMapping("/{id}/investigate")
    public ResponseEntity<Case> investigate(@PathVariable Integer id) {
        return alertManager.investigate(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Case> close(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        String resolutionNotes = body != null ? body.get("resolutionNotes") : null;
        ResolutionReasonCode reasonCode = parseReasonCode(body);
        return alertManager.close(id, resolutionNotes, reasonCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** "Dismiss as False Positive" - a terminal state distinct from CLOSED. */
    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Case> dismiss(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        String resolutionNotes = body != null ? body.get("resolutionNotes") : null;
        ResolutionReasonCode reasonCode = parseReasonCode(body);
        return alertManager.dismiss(id, resolutionNotes, reasonCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Parses the optional "resolutionReasonCode" field (fixed list - see enums.ResolutionReasonCode); returns null (not set) if absent or blank. */
    private ResolutionReasonCode parseReasonCode(Map<String, String> body) {
        if (body == null) {
            return null;
        }
        String raw = body.get("resolutionReasonCode");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResolutionReasonCode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown resolutionReasonCode: " + raw);
        }
    }

    /** Aggregate stats for the dashboard - count by status plus average time-to-acknowledge/close, in minutes. */
    @GetMapping("/stats")
    public ResponseEntity<CaseStatsResponse> stats() {
        Map<String, Long> countByStatus = caseRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(row -> ((CaseStatus) row[0]).name(), row -> (Long) row[1]));

        CaseStatsResponse response = new CaseStatsResponse();
        response.setCountByStatus(countByStatus);
        response.setAvgMinutesToAcknowledge(averageMinutes(caseRepository.findCreatedAndAcknowledgedTimestamps()));
        response.setAvgMinutesToClose(averageMinutes(caseRepository.findCreatedAndClosedTimestamps()));
        response.setTotalCases(caseRepository.count());
        return ResponseEntity.ok(response);
    }

    /** A rejected lifecycle transition (e.g. close-without-acknowledge, or acting on an already-terminal case) is a 409, not a 500. */
    @ExceptionHandler(InvalidCaseTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(InvalidCaseTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    /** An unrecognized resolutionReasonCode value is a 400, not a 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Double averageMinutes(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.stream()
                .mapToLong(r -> Duration.between((LocalDateTime) r[0], (LocalDateTime) r[1]).toMinutes())
                .average()
                .orElse(0.0);
    }
}
