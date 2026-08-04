package com.example.enums;

/**
 * State of an operator-submitted "Run Analysis Now" request (see
 * entity.NetworkRunRequest). The Python job polls for PENDING rows on its
 * own schedule - this is a DB-mediated trigger, not a direct HTTP call from
 * Spring into the Python process, keeping the two components decoupled.
 */
public enum NetworkRunRequestStatus {
    PENDING,
    PICKED_UP,
    DONE
}
