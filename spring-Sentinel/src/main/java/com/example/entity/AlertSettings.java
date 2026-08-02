package com.example.entity;

import jakarta.persistence.*;

/**
 * DB-backed replacement for the previously hardcoded AlertConfig (severity
 * bands, alert-creation threshold, case merge cooldown). Single-row table -
 * id is always 1 - editable via AlertSettingsController so an operator can
 * tune these without a redeploy, mirroring how 'rules' already works.
 */
@Entity
@Table(name = "alert_settings")
public class AlertSettings {

    @Id
    private Integer id;

    @Column(name = "min_score_to_create_alert", nullable = false)
    private int minScoreToCreateAlert;

    @Column(name = "low_severity_max", nullable = false)
    private int lowSeverityMax;

    @Column(name = "medium_severity_max", nullable = false)
    private int mediumSeverityMax;

    @Column(name = "merge_cooldown_minutes", nullable = false)
    private int mergeCooldownMinutes;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getMinScoreToCreateAlert() { return minScoreToCreateAlert; }
    public void setMinScoreToCreateAlert(int minScoreToCreateAlert) { this.minScoreToCreateAlert = minScoreToCreateAlert; }
    public int getLowSeverityMax() { return lowSeverityMax; }
    public void setLowSeverityMax(int lowSeverityMax) { this.lowSeverityMax = lowSeverityMax; }
    public int getMediumSeverityMax() { return mediumSeverityMax; }
    public void setMediumSeverityMax(int mediumSeverityMax) { this.mediumSeverityMax = mediumSeverityMax; }
    public int getMergeCooldownMinutes() { return mergeCooldownMinutes; }
    public void setMergeCooldownMinutes(int mergeCooldownMinutes) { this.mergeCooldownMinutes = mergeCooldownMinutes; }
}
