/**
 * TransactionCreatedEvent
 * 
 * PURPOSE: A message/event object that gets fired whenever a new transaction
 *          is saved to the database. This is part of our internal messaging system
 *          (the 'queue' that doesn't need external infrastructure).
 * 
 * HOW IT WORKS:
 *   1. TransactionService saves a transaction to DB
 *   2. TransactionService wraps the saved transaction in this event
 *   3. TransactionService publishes the event
 *   4. TransactionEventListener picks it up on a background thread
 *   5. Later: Rule Engine evaluates the transaction and creates alerts
 * 
 * BENEFITS: Decouples transaction saving from rule evaluation. Saving doesn't
 *           have to wait for rule checks. Everything happens asynchronously.
 * 
 * PAYLOAD: Carries the saved Transaction object so the listener knows
 *          which transaction to process.
 */
package com.example.event;

import com.example.entity.Transaction;
import org.springframework.context.ApplicationEvent;

public class TransactionCreatedEvent extends ApplicationEvent {

    private final Transaction transaction;

    public TransactionCreatedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
