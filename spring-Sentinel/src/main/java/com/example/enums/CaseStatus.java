package com.example.enums;

/** Investigation lifecycle status. Matches the 'status' ENUM used by both the 'cases' and 'alerts' tables. */
public enum CaseStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    DISMISSED,
    IN_REVIEW,
    ESCALATED,
    CLOSED
}
