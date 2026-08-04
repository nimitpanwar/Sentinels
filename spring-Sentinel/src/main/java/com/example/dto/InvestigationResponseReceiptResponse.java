package com.example.dto;

import java.time.LocalDateTime;

public class InvestigationResponseReceiptResponse {
    private Integer responseId;
    private Integer messageId;
    private LocalDateTime submittedAt;
    private String message;

    public Integer getResponseId() { return responseId; }
    public void setResponseId(Integer responseId) { this.responseId = responseId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}