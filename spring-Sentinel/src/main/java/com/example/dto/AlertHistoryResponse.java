package com.example.dto;

import com.example.enums.CaseStatus;
import com.example.enums.ResolutionReasonCode;
import com.example.enums.Severity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertHistoryResponse {
    private Integer alertId;
    private Integer caseId;
    private Severity severity;
    private CaseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime dismissedAt;
    private String resolutionNotes;
    private ResolutionReasonCode resolutionReasonCode;
    private List<AlertHistoryEventResponse> events = new ArrayList<>();

    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(LocalDateTime dismissedAt) { this.dismissedAt = dismissedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public ResolutionReasonCode getResolutionReasonCode() { return resolutionReasonCode; }
    public void setResolutionReasonCode(ResolutionReasonCode resolutionReasonCode) { this.resolutionReasonCode = resolutionReasonCode; }
    public List<AlertHistoryEventResponse> getEvents() { return events; }
    public void setEvents(List<AlertHistoryEventResponse> events) { this.events = events; }
}
