/**
 * TransactionEventListener
 * 
 * PURPOSE: Listens for TransactionCreatedEvent on a background thread and
 *          currently writes transaction details to a log file. Later, this
 *          will be replaced with Rule Engine evaluation logic.
 * 
 * HOW IT WORKS:
 *   - Marked with @Async to run on a separate thread pool (non-blocking)
 *   - When TransactionCreatedEvent is published, this method is triggered
 *   - Currently logs transaction to 'transactions_log.txt' for visibility
 * 
 * TEMPORARY BEHAVIOR:
 *   The code between the TEMP markers will be replaced when the Rule Engine
 *   is ready. The entire writeTransactionToFile() method will be removed.
 * 
 * FUTURE BEHAVIOR:
 *   - Will call ruleEngineService.evaluate(transaction)
 *   - Rule Engine will check all rules and create alerts if any rules trigger
 * 
 * KEY INSIGHT: This is where rule evaluation happens, completely separate
 *              from transaction creation. By the time this runs, the transaction
 *              is already safely stored in the database.
 */
package com.example.event;

import com.example.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// NOTE: Lombok (@Slf4j) intentionally not used - see entity/Transaction.java note.
// NOTE: Dead code - no one publishes TransactionCreatedEvent since evaluation
// runs synchronously from TransactionService/RiskEvaluationService. Kept
// compiling for now, gutted of the removed Transaction fields (timestamp/
// source no longer exist on the real relational entity).
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    /**
     * Picks up TransactionCreatedEvent on a background thread. Currently unused
     * since transaction evaluation happens synchronously - kept for compatibility.
     */
    @Async
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();
        log.debug("TransactionEventListener received event for transaction: {}", tx.getTransactionId());
    }
}

