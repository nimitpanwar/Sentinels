package com.example.dto;

public class InvestigationResponseFormRequest {
    private Boolean recognizedTransaction;
    private Boolean authorizedTransaction;
    private String explanation;
    private String respondentName;
    private String respondentEmail;

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
}