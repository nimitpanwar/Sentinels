package com.example.dto;

/** Request payload for composing and sending a customer investigation outreach email. */
public class InvestigationSendRequest {
    private String subject;
    private String body;

    public InvestigationSendRequest() {
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
