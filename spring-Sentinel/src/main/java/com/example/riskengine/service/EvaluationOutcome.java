package com.example.riskengine.service;

import com.example.entity.Alert;
import com.example.riskengine.model.RiskResult;

import java.util.Optional;

/** Combined result of a single transaction's risk evaluation: the score/rule breakdown plus any alert produced. */
public class EvaluationOutcome {

    private final RiskResult riskResult;
    private final Optional<Alert> alert;

    public EvaluationOutcome(RiskResult riskResult, Optional<Alert> alert) {
        this.riskResult = riskResult;
        this.alert = alert;
    }

    public RiskResult getRiskResult() { return riskResult; }
    public Optional<Alert> getAlert() { return alert; }
}
