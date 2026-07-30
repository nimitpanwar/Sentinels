/**
 * TransactionType Enum
 * 
 * PURPOSE: Represents the direction of money flow in a transaction.
 *   - DEBIT: Money going out from an account
 *   - CREDIT: Money coming into an account
 * 
 * USAGE: Every transaction must have exactly one TransactionType value.
 *        This helps classify whether funds are being sent or received.
 */
package com.example.enums;

public enum TransactionType {
    DEBIT,
    CREDIT
}
