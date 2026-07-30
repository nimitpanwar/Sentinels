package com.example.entity;

import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the real 'transactions' table (final relational schema
 * provided by the user - see customers/accounts/payees/transactions/...).
 * No Lombok - it silently fails to generate methods on this JDK (JDK 25),
 * so everything below is written out explicitly.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_txn_account_timestamp", columnList = "account_id, transaction_timestamp"),
        @Index(name = "idx_txn_payee", columnList = "payee_id")
    }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payee_id", nullable = false)
    private Payee payee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 255)
    private String description;

    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;

    @Column(length = 150)
    private String location;

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Transaction() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (transactionTimestamp == null) {
            transactionTimestamp = LocalDateTime.now();
        }
    }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Payee getPayee() { return payee; }
    public void setPayee(Payee payee) { this.payee = payee; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getTransactionTimestamp() { return transactionTimestamp; }
    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) { this.transactionTimestamp = transactionTimestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Convenience accessor - avoids null checks scattered across rule code. */
    public Integer getAccountId() { return account != null ? account.getAccountId() : null; }

    /** Convenience accessor - avoids null checks scattered across rule code. */
    public Integer getPayeeId() { return payee != null ? payee.getPayeeId() : null; }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written stand-in for Lombok's @Builder (see class-level note). */
    public static class Builder {
        private final Transaction tx = new Transaction();

        public Builder account(Account account) { tx.account = account; return this; }
        public Builder payee(Payee payee) { tx.payee = payee; return this; }
        public Builder amount(BigDecimal amount) { tx.amount = amount; return this; }
        public Builder currency(String currency) { tx.currency = currency; return this; }
        public Builder type(TransactionType type) { tx.type = type; return this; }
        public Builder status(TransactionStatus status) { tx.status = status; return this; }
        public Builder description(String description) { tx.description = description; return this; }
        public Builder merchantCategory(String merchantCategory) { tx.merchantCategory = merchantCategory; return this; }
        public Builder location(String location) { tx.location = location; return this; }
        public Builder transactionTimestamp(LocalDateTime transactionTimestamp) { tx.transactionTimestamp = transactionTimestamp; return this; }
        public Builder createdAt(LocalDateTime createdAt) { tx.createdAt = createdAt; return this; }

        public Transaction build() { return tx; }
    }
}
