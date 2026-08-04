package com.example.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
    name = "investigation_responses",
    indexes = {
        @Index(name = "idx_inv_resp_msg_created", columnList = "message_id, created_at")
    }
)
public class InvestigationResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Integer responseId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private InvestigationMessage message;

    @Column(name = "recognized_transaction", nullable = false)
    private Boolean recognizedTransaction;

    @Column(name = "authorized_transaction", nullable = false)
    private Boolean authorizedTransaction;

    @Lob
    @Column(name = "explanation", nullable = false)
    private String explanation;

    @Column(name = "respondent_name", nullable = false, length = 150)
    private String respondentName;

    @Column(name = "respondent_email", nullable = false, length = 150)
    private String respondentEmail;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Lob
    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InvestigationResponse() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (submittedAt == null) {
            submittedAt = now;
        }
    }

    public Integer getResponseId() { return responseId; }
    public void setResponseId(Integer responseId) { this.responseId = responseId; }
    public InvestigationMessage getMessage() { return message; }
    public void setMessage(InvestigationMessage message) { this.message = message; }
    public Boolean getRecognizedTransaction() { return recognizedTransaction; }
    public void setRecognizedTransaction(Boolean recognizedTransaction) { this.recognizedTransaction = recognizedTransaction; }
    public Boolean getAuthorizedTransaction() { return authorizedTransaction; }
    public void setAuthorizedTransaction(Boolean authorizedTransaction) { this.authorizedTransaction = authorizedTransaction; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRespondentName() { return respondentName; }
    public void setRespondentName(String respondentName) { this.respondentName = respondentName; }
    public String getRespondentEmail() { return respondentEmail; }
    public void setRespondentEmail(String respondentEmail) { this.respondentEmail = respondentEmail; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}