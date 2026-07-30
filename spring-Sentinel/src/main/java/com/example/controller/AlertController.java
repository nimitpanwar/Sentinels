package com.example.controller;

import com.example.entity.Alert;
import com.example.repository.AlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
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
}

