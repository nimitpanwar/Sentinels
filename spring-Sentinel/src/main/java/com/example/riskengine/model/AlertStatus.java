package com.example.riskengine.model;

/** Alert lifecycle states. Ported from backend/ unchanged. */
public enum AlertStatus {
    OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED
}
