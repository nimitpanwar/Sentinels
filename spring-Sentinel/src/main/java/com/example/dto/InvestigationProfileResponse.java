package com.example.dto;

import java.util.ArrayList;
import java.util.List;

public class InvestigationProfileResponse {
    private Integer alertId;
    private Integer caseId;
    private String severity;
    private List<String> requiredSteps = new ArrayList<>();
    private List<String> completedSteps = new ArrayList<>();
    private List<String> blockedReasons = new ArrayList<>();
    private Integer cooldownRemainingSeconds;
    private Boolean allowSkipCooldown;

    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public List<String> getRequiredSteps() { return requiredSteps; }
    public void setRequiredSteps(List<String> requiredSteps) { this.requiredSteps = requiredSteps; }
    public List<String> getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(List<String> completedSteps) { this.completedSteps = completedSteps; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public void setBlockedReasons(List<String> blockedReasons) { this.blockedReasons = blockedReasons; }
    public Integer getCooldownRemainingSeconds() { return cooldownRemainingSeconds; }
    public void setCooldownRemainingSeconds(Integer cooldownRemainingSeconds) { this.cooldownRemainingSeconds = cooldownRemainingSeconds; }
    public Boolean getAllowSkipCooldown() { return allowSkipCooldown; }
    public void setAllowSkipCooldown(Boolean allowSkipCooldown) { this.allowSkipCooldown = allowSkipCooldown; }
}
