package com.example.entity;

import com.example.enums.InvestigationMessageStatus;
import com.example.enums.InvestigationResponseStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Persisted outreach message sent by an analyst during investigation.
 * This is the audit thread entry shown in the alert detail Investigation tab.
 */
@Entity
@Table(
    name = "investigation_messages",
    indexes = {
        @Index(name = "idx_inv_msg_alert_created", columnList = "alert_id, created_at"),
        @Index(name = "idx_inv_msg_case_created", columnList = "case_id, created_at")
    }
)
public class InvestigationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @Column(name = "intended_recipient_email", length = 150)
    private String intendedRecipientEmail;

    @Column(name = "delivered_recipient_email", nullable = false, length = 150)
    private String deliveredRecipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(name = "body_snapshot", nullable = false)
    private String bodySnapshot;

    @Column(name = "response_token", nullable = false, length = 80, unique = true)
    private String responseToken;

    @Column(name = "response_token_expires_at", nullable = false)
    private LocalDateTime responseTokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 25)
    private InvestigationResponseStatus responseStatus = InvestigationResponseStatus.AWAITING_RESPONSE;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "token_consumed_at")
    private LocalDateTime tokenConsumedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 15)
    private InvestigationMessageStatus deliveryStatus = InvestigationMessageStatus.PENDING;

    @Column(name = "delivery_error", length = 500)
    private String deliveryError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public InvestigationMessage() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }
    public Case getCase() { return aCase; }
    public void setCase(Case aCase) { this.aCase = aCase; }
    public String getIntendedRecipientEmail() { return intendedRecipientEmail; }
    public void setIntendedRecipientEmail(String intendedRecipientEmail) { this.intendedRecipientEmail = intendedRecipientEmail; }
    public String getDeliveredRecipientEmail() { return deliveredRecipientEmail; }
    public void setDeliveredRecipientEmail(String deliveredRecipientEmail) { this.deliveredRecipientEmail = deliveredRecipientEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodySnapshot() { return bodySnapshot; }
    public void setBodySnapshot(String bodySnapshot) { this.bodySnapshot = bodySnapshot; }
    public String getResponseToken() { return responseToken; }
    public void setResponseToken(String responseToken) { this.responseToken = responseToken; }
    public LocalDateTime getResponseTokenExpiresAt() { return responseTokenExpiresAt; }
    public void setResponseTokenExpiresAt(LocalDateTime responseTokenExpiresAt) { this.responseTokenExpiresAt = responseTokenExpiresAt; }
    public InvestigationResponseStatus getResponseStatus() { return responseStatus; }
    public void setResponseStatus(InvestigationResponseStatus responseStatus) { this.responseStatus = responseStatus; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public LocalDateTime getTokenConsumedAt() { return tokenConsumedAt; }
    public void setTokenConsumedAt(LocalDateTime tokenConsumedAt) { this.tokenConsumedAt = tokenConsumedAt; }
    public InvestigationMessageStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(InvestigationMessageStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String deliveryError) { this.deliveryError = deliveryError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
