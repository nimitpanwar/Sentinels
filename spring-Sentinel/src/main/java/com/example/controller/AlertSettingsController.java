package com.example.controller;

import com.example.entity.AlertSettings;
import com.example.repository.AlertSettingsRepository;
import com.example.riskengine.config.AlertConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read/edit endpoint for the single alert-settings row (severity bands,
 * alert-creation threshold, case merge cooldown). See AlertConfig, which
 * reads this same row (cached) for the values RiskEngine/AlertManager use.
 */
@RestController
@RequestMapping("/api/alert-settings")
public class AlertSettingsController {

    private static final Integer SETTINGS_ID = 1;

    private final AlertSettingsRepository repository;
    private final AlertConfig alertConfig;

    public AlertSettingsController(AlertSettingsRepository repository, AlertConfig alertConfig) {
        this.repository = repository;
        this.alertConfig = alertConfig;
    }

    /** Delegates to AlertConfig.getSettings() so a fresh install returns seeded defaults instead of 404. */
    @GetMapping
    public ResponseEntity<AlertSettings> get() {
        return ResponseEntity.ok(alertConfig.getSettings());
    }

    @PutMapping
    @CacheEvict(value = "alertSettings", allEntries = true)
    public ResponseEntity<AlertSettings> update(@RequestBody AlertSettings request) {
        AlertSettings settings = repository.findById(SETTINGS_ID).orElseGet(AlertSettings::new);
        settings.setId(SETTINGS_ID);
        settings.setMinScoreToCreateAlert(request.getMinScoreToCreateAlert());
        settings.setLowSeverityMax(request.getLowSeverityMax());
        settings.setMediumSeverityMax(request.getMediumSeverityMax());
        settings.setMergeCooldownMinutes(request.getMergeCooldownMinutes());
        return ResponseEntity.ok(repository.save(settings));
    }
}
