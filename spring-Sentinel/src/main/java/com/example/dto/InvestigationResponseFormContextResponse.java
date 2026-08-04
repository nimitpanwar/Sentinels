package com.example.dto;

import java.time.LocalDateTime;

public class InvestigationResponseFormContextResponse {
    private Integer messageId;
    private Integer alertId;
    private Integer caseId;
    private String recipientEmailMasked;
    private String subject;
    private LocalDateTime sentAt;
    private LocalDateTime responseTokenExpiresAt;
    private boolean alreadySubmitted;

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public String getRecipientEmailMasked() { return recipientEmailMasked; }
    public void setRecipientEmailMasked(String recipientEmailMasked) { this.recipientEmailMasked = recipientEmailMasked; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getResponseTokenExpiresAt() { return responseTokenExpiresAt; }
    public void setResponseTokenExpiresAt(LocalDateTime responseTokenExpiresAt) { this.responseTokenExpiresAt = responseTokenExpiresAt; }
    public boolean isAlreadySubmitted() { return alreadySubmitted; }
    public void setAlreadySubmitted(boolean alreadySubmitted) { this.alreadySubmitted = alreadySubmitted; }
}