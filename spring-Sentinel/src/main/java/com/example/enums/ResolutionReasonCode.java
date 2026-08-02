package com.example.enums;

/**
 * Fixed reason-code list recorded alongside resolutionNotes whenever a Case is
 * moved to CLOSED or DISMISSED. Needed as structured (queryable) data rather
 * than free text so downstream consumers - notably the network-analysis batch
 * job's personalized PageRank seeding, which needs a reliable "confirmed
 * fraud" account set - don't have to parse resolutionNotes text.
 * See notes/RiskLog.md for the original fixed-list proposal this implements.
 */
public enum ResolutionReasonCode {
    CONFIRMED_FRAUD,
    FALSE_POSITIVE_KNOWN_CUSTOMER,
    FALSE_POSITIVE_RULE_TOO_SENSITIVE,
    LEGITIMATE_LARGE_PURCHASE,
    DUPLICATE_ALERT,
    INSUFFICIENT_EVIDENCE
}
