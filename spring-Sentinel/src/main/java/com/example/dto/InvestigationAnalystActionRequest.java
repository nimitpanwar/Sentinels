package com.example.dto;

public class InvestigationAnalystActionRequest {
    private String action;
    private String analystNotes;
    private String subject;
    private String body;
    private String updateScope;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getAnalystNotes() { return analystNotes; }
    public void setAnalystNotes(String analystNotes) { this.analystNotes = analystNotes; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getUpdateScope() { return updateScope; }
    public void setUpdateScope(String updateScope) { this.updateScope = updateScope; }
}