/**
 * TransactionEventListener
 * 
 * PURPOSE: Picks up TransactionCreatedEvent asynchronously and performs the
 *          actual rule evaluation + alert generation, completely decoupled
 *          from the request/response cycle that recorded the transaction.
 * 
 * HOW IT WORKS:
 *   - @TransactionalEventListener(phase = AFTER_COMMIT) defers this until
 *     the transaction that saved the Transaction row has actually committed,
 *     so evaluation never runs against a row that could still be rolled back.
 *   - @Async("taskExecutor") routes the call onto the background thread pool
 *     configured in AsyncConfig, so it never blocks the caller.
 * 
 * KEY INSIGHT: This is where rule evaluation happens, completely separate
 *              from transaction creation. By the time this runs, the
 *              transaction is already safely committed to the database.
 */
package com.example.event;

import com.example.entity.Transaction;
import com.example.riskengine.service.RiskEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// NOTE: Lombok (@Slf4j/@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private final RiskEvaluationService riskEvaluationService;

    public TransactionEventListener(RiskEvaluationService riskEvaluationService) {
        this.riskEvaluationService = riskEvaluationService;
    }

    /**
     * Runs on a background thread ({@code taskExecutor}) only after the
     * transaction that created this event has committed. Failures are caught
     * here (rather than left to propagate on the async thread) so a bad rule
     * evaluation can never affect the already-recorded transaction - it's
     * also independently tracked via the TransactionQueueStatus=FAILED audit
     * row written inside RiskEvaluationService.
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();
        try {
            riskEvaluationService.evaluate(tx);
            log.debug("Async evaluation completed for transaction {}", tx.getTransactionId());
        } catch (RuntimeException ex) {
            log.error("Async evaluation failed for transaction {}: {}", tx.getTransactionId(), ex.getMessage(), ex);
        }
    }
}

