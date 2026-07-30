/**
 * TransactionResponse DTO (Data Transfer Object)
 * 
 * PURPOSE: Defines the shape of data that the API sends back to callers
 *          after creating or retrieving a transaction.
 * 
 * FIELDS: Includes EVERYTHING—both what the caller sent plus system-generated fields:
 *   - id: Database ID
 *   - transactionId: Unique transaction reference (e.g., TXN-A3F8C12D01)
 *   - accountId, payeeId, amount, currency, type: From the request
 *   - timestamp: When it happened
 *   - status: COMPLETED/PENDING/FAILED
 *   - createdAt: When it was saved to database
 *   - source: API or SIMULATOR
 * 
 * WHY ALL FIELDS: The frontend needs to see everything about a transaction,
 *                 including the auto-generated fields and current status.
 * 
 * WHY SEPARATE: DTOs act as a contract between backend and frontend.
 *               The frontend gets only what's meant for display/use.
 */
package com.example.dto;

import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private Instant timestamp;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;
    private TransactionSource source;
}
