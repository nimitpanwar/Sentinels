package com.example.dto;

public class InvestigationProfileUpdateRequest {
    private String analystNote;
    private Boolean checklistComplete;

    public String getAnalystNote() { return analystNote; }
    public void setAnalystNote(String analystNote) { this.analystNote = analystNote; }
    public Boolean getChecklistComplete() { return checklistComplete; }
    public void setChecklistComplete(Boolean checklistComplete) { this.checklistComplete = checklistComplete; }
}
