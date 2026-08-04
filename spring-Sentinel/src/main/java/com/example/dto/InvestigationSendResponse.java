package com.example.dto;

import com.example.entity.Alert;

/** Response payload after sending an investigation email. */
public class InvestigationSendResponse {
    private Alert alert;
    private InvestigationMessageResponse message;

    public InvestigationSendResponse() {
    }

    public InvestigationSendResponse(Alert alert, InvestigationMessageResponse message) {
        this.alert = alert;
        this.message = message;
    }

    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }
    public InvestigationMessageResponse getMessage() { return message; }
    public void setMessage(InvestigationMessageResponse message) { this.message = message; }
}
