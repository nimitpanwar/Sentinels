/**
 * TransactionRequest DTO (Data Transfer Object)
 * 
 * PURPOSE: Defines the shape of data that an API caller sends when creating
 *          a transaction via REST endpoint (POST /api/transactions).
 * 
 * FIELDS:
 *   - accountId: Which account is making this transaction
 *   - payeeId: Who is receiving or sending money
 *   - amount: How much money
 *   - currency: Which currency (e.g., USD)
 *   - type: DEBIT or CREDIT
 *   - description: Optional note about the transaction
 * 
 * NOTE: System-generated fields like transactionId, timestamp, and createdAt
 *       are NOT included here—the service layer creates those automatically.
 * 
 * WHY SEPARATE: Keeps API contracts clean. Callers only send what they need;
 *               the server fills in the rest (like timestamps).
 */
package com.example.dto;

import com.example.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private String description;
}
