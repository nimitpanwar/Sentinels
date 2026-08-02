package com.example.dto;

import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Optional filter criteria for GET /api/transactions (Appendix C "Transactions
 * List Screen": search by ID/description, filter by date range/account/amount
 * range). Any null field means "no constraint" - see TransactionSpecifications.
 */
public record TransactionFilter(
        Integer accountId,
        Integer payeeId,
        TransactionStatus status,
        TransactionType type,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDateTime from,
        LocalDateTime to,
        String search
) {
}
