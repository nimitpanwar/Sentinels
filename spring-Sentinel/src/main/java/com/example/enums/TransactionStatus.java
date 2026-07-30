/**
 * TransactionStatus Enum
 * 
 * PURPOSE: Represents the current state of a transaction in the system.
 *   - COMPLETED: Transaction went through successfully
 *   - PENDING: Transaction is waiting to be processed
 *   - FAILED: Transaction did not go through (error/rejection)
 * 
 * USAGE: Tracks the lifecycle of transactions so we know which ones are valid
 *        and which ones still need attention or investigation.
 */
package com.example.enums;

public enum TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED
}
