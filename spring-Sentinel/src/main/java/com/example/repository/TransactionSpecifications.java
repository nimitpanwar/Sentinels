package com.example.repository;

import com.example.entity.Transaction;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dynamic, optional filters for GET /api/transactions, combined via
 * Specification.where(...).and(...) in TransactionService#getTransactions.
 * Each method returns null (no constraint) when its argument is null/blank -
 * Spring Data's Specification composition treats a null predicate as "true"
 * and simply omits that condition from the final query.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> hasAccountId(Integer accountId) {
        return (root, query, cb) -> accountId == null ? null : cb.equal(root.get("account").get("accountId"), accountId);
    }

    public static Specification<Transaction> hasPayeeId(Integer payeeId) {
        return (root, query, cb) -> payeeId == null ? null : cb.equal(root.get("payee").get("payeeId"), payeeId);
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> amountAtLeast(BigDecimal min) {
        return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Transaction> amountAtMost(BigDecimal max) {
        return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<Transaction> timestampFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("transactionTimestamp"), from);
    }

    public static Specification<Transaction> timestampTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("transactionTimestamp"), to);
    }

    /** Matches an exact transaction ID (if search parses as one) OR a case-insensitive description substring. */
    public static Specification<Transaction> search(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> null;
        }
        String likePattern = "%" + search.trim().toLowerCase() + "%";
        Integer asId = parseIntOrNull(search.trim());

        return (root, query, cb) -> {
            var descriptionMatch = cb.like(cb.lower(root.get("description")), likePattern);
            return asId != null ? cb.or(cb.equal(root.get("transactionId"), asId), descriptionMatch) : descriptionMatch;
        };
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
