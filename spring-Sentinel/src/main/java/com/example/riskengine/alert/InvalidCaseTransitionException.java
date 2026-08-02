package com.example.riskengine.alert;

import com.example.enums.CaseStatus;

/**
 * Thrown when a requested case status transition isn't allowed by the
 * lifecycle state machine (see AlertManager.ALLOWED_TRANSITIONS) - e.g.
 * closing a case without acknowledging it first, or acting on a case that's
 * already CLOSED/DISMISSED (both terminal, no outgoing transitions).
 * Translated to HTTP 409 Conflict by CaseController.
 */
public class InvalidCaseTransitionException extends RuntimeException {

    public InvalidCaseTransitionException(CaseStatus from, CaseStatus to) {
        super("Cannot transition case from " + from + " to " + to);
    }
}
