package com.example.riskengine.config;

import com.example.entity.AlertSettings;
import com.example.enums.Severity;
import com.example.repository.AlertSettingsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * DB-backed alert/case configuration (severity bands, creation threshold,
 * merge/cooldown window), editable at runtime via AlertSettingsController -
 * see entity/AlertSettings.java. Previously hardcoded; kept as the same
 * class/method signatures so AlertManager (its only caller) needed no changes.
 * Falls back to sensible defaults (and persists them) if the settings row
 * doesn't exist yet, so a fresh DB still works out of the box.
 */
@Component
public class AlertConfig {

    private static final Integer SETTINGS_ID = 1;

    private final AlertSettingsRepository settingsRepository;

    public AlertConfig(AlertSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    // Read on every alert-worthy transaction evaluation (see AlertManager) -
    // cached with a short TTL (see CacheConfig), evicted by
    // AlertSettingsController on update so edits take effect immediately.
    @Cacheable("alertSettings")
    public AlertSettings getSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(this::seedDefaults);
    }

    private AlertSettings seedDefaults() {
        AlertSettings settings = new AlertSettings();
        settings.setId(SETTINGS_ID);
        settings.setMinScoreToCreateAlert(50); // scores below this are ignored, no alert created
        settings.setLowSeverityMax(60);          // 0-60   = LOW
        settings.setMediumSeverityMax(80);       // 61-80  = MID, 81-100 = HIGH
        settings.setMergeCooldownMinutes(60);    // window to merge new activity into an existing open case
        return settingsRepository.save(settings);
    }

    public int getMinScoreToCreateAlert() { return getSettings().getMinScoreToCreateAlert(); }
    public int getMergeCooldownMinutes() { return getSettings().getMergeCooldownMinutes(); }

    public Severity severityFor(int riskScore) {
        AlertSettings settings = getSettings();
        if (riskScore <= settings.getLowSeverityMax()) return Severity.LOW;
        if (riskScore <= settings.getMediumSeverityMax()) return Severity.MID;
        return Severity.HIGH;
    }
}

