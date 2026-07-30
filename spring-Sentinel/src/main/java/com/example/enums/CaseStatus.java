package com.example.enums;

/** Investigation lifecycle status. Matches the 'status' ENUM used by both the 'cases' and 'alerts' tables. */
public enum CaseStatus {
    OPEN,
    IN_REVIEW,
    ESCALATED,
    CLOSED
}
