package com.example.enums;

/**
 * Investigation lifecycle status. Matches the 'status' ENUM used by both the
 * 'cases' and 'alerts' tables.
 *
 * Workflow: OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED, with DISMISSED
 * reachable from ACKNOWLEDGED or INVESTIGATING (false positive / no action
 * needed). CLOSED and DISMISSED are terminal - see AlertManager's transition
 * validation for the enforced state machine.
 */
public enum CaseStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    CLOSED,
    DISMISSED
}
