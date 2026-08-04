package com.example.dto;

import com.example.enums.InvestigationMessageStatus;

import java.time.LocalDateTime;

/** API response representation of one investigation outreach message thread item. */
public class InvestigationMessageResponse {
    private Integer messageId;
    private Integer alertId;
    private Integer caseId;
    private String intendedRecipientEmail;
    private String deliveredRecipientEmail;
    private String subject;
    private String bodySnapshot;
    private InvestigationMessageStatus deliveryStatus;
    private String deliveryError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime responseTokenExpiresAt;

    public InvestigationMessageResponse() {
    }

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    public Integer getCaseId() { return caseId; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public String getIntendedRecipientEmail() { return intendedRecipientEmail; }
    public void setIntendedRecipientEmail(String intendedRecipientEmail) { this.intendedRecipientEmail = intendedRecipientEmail; }
    public String getDeliveredRecipientEmail() { return deliveredRecipientEmail; }
    public void setDeliveredRecipientEmail(String deliveredRecipientEmail) { this.deliveredRecipientEmail = deliveredRecipientEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodySnapshot() { return bodySnapshot; }
    public void setBodySnapshot(String bodySnapshot) { this.bodySnapshot = bodySnapshot; }
    public InvestigationMessageStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(InvestigationMessageStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String deliveryError) { this.deliveryError = deliveryError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getResponseTokenExpiresAt() { return responseTokenExpiresAt; }
    public void setResponseTokenExpiresAt(LocalDateTime responseTokenExpiresAt) { this.responseTokenExpiresAt = responseTokenExpiresAt; }
}
