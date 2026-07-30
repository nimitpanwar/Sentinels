package com.example.controller;

import com.example.entity.Case;
import com.example.repository.CaseRepository;
import com.example.riskengine.alert.AlertManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    public CaseController(CaseRepository caseRepository, AlertManager alertManager) {
        this.caseRepository = caseRepository;
        this.alertManager = alertManager;
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

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Case> acknowledge(@PathVariable Integer id) {
        return alertManager.acknowledge(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/escalate")
    public ResponseEntity<Case> escalate(@PathVariable Integer id) {
        return alertManager.escalate(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Case> close(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        String resolutionNotes = body != null ? body.get("resolutionNotes") : null;
        return alertManager.close(id, resolutionNotes)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
