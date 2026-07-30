package com.example.enums;

/** Matches the 'queue_status' ENUM in the 'transaction_queue_status' table. */
public enum QueueStatus {
    PENDING,
    PROCESSING,
    EVALUATED,
    FAILED
}
