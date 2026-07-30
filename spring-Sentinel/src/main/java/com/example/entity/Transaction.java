package com.example.entity;

import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_account_timestamp", columnList = "account_id, timestamp"),
        @Index(name = "idx_account_payee",     columnList = "account_id, payee_id"),
        @Index(name = "idx_timestamp",          columnList = "timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "payee_id", nullable = false, length = 50)
    private String payeeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionSource source;

    @PrePersist
    public void prePersist() {//Generates a transaction_id and timestamp if already doesnt exist.
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (transactionId == null) {
            transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        }
    }
}
