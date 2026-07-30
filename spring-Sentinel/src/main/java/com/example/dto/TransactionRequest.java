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

import java.math.BigDecimal;

// NOTE: Lombok (@Data) intentionally not used - see entity/Transaction.java note.
public class TransactionRequest {

    private Integer accountId;
    private Integer payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private String description;
    private String location;
    private String merchantCategory;

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }
    public Integer getPayeeId() { return payeeId; }
    public void setPayeeId(Integer payeeId) { this.payeeId = payeeId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
}
