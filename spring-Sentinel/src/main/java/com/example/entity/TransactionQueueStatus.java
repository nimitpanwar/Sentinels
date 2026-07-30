package com.example.entity;

import com.example.enums.QueueStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the 'transaction_queue_status' table - an audit trail of
 * evaluation lifecycle per transaction. Evaluation itself is still
 * SYNCHRONOUS (the score is returned in the same POST response); this table
 * just records that fact for history/observability, it does not drive an
 * actual async queue.
 */
@Entity
@Table(name = "transaction_queue_status")
public class TransactionQueueStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 15)
    private QueueStatus queueStatus = QueueStatus.PENDING;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public TransactionQueueStatus() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public QueueStatus getQueueStatus() { return queueStatus; }
    public void setQueueStatus(QueueStatus queueStatus) { this.queueStatus = queueStatus; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
