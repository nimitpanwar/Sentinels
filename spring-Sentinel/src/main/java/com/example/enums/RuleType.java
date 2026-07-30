package com.example.enums;

/**
 * The catalogue of rule types supported by the risk engine. Matches the
 * 'rule_type' ENUM in the 'rules' table exactly. DEVICE_CHANGE has no Java
 * rule implementation bound to it (deviceId tracking was intentionally
 * dropped) - it exists here only so the DB enum/schema stays complete; any
 * row seeded with this type is left inactive and simply never evaluated.
 */
public enum RuleType {
    AMOUNT_ANOMALY,
    VELOCITY,
    NEW_PAYEE,
    TIME_ANOMALY,
    DEVICE_CHANGE,
    LOCATION_CHANGE,
    SPENDING_PATTERN
}
