package com.example.dto;

public class HighRiskSelfApprovalRequest {
    private String justification;
    private Boolean confirmOne;
    private Boolean confirmTwo;
    private Boolean skipCooldown;

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public Boolean getConfirmOne() { return confirmOne; }
    public void setConfirmOne(Boolean confirmOne) { this.confirmOne = confirmOne; }
    public Boolean getConfirmTwo() { return confirmTwo; }
    public void setConfirmTwo(Boolean confirmTwo) { this.confirmTwo = confirmTwo; }
    public Boolean getSkipCooldown() { return skipCooldown; }
    public void setSkipCooldown(Boolean skipCooldown) { this.skipCooldown = skipCooldown; }
}
