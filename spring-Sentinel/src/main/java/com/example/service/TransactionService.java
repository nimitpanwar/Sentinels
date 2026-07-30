/**
 * TransactionService
 * 
 * PURPOSE: The central service for all transaction operations. Handles creating,
 *          retrieving, and publishing transactions. This is the main business logic
 *          class that connects the API to the database and event system.
 * 
 * KEY METHODS:
 *   - createTransaction(request, source): Creates a new transaction, saves it to DB,
 *     fires an event, and returns the saved transaction to the caller.
 *   - getAllTransactions(): Fetches all transactions from the database.
 *   - getTransactionById(id): Fetches a single transaction by ID.
 * 
 * IMPORTANT: Both the API controller AND the simulator use this same method.
 *            They only differ in the 'source' parameter (API vs SIMULATOR).
 *            This ensures consistent handling regardless of where transactions come from.
 * 
 * EVENT PUBLISHING: After every save, it publishes TransactionCreatedEvent.
 *                   This triggers the event listener on a background thread
 *                   (which will eventually evaluate rules).
 * 
 * DATABASE ACCESS: Only talks to the database through TransactionRepository.
 *                  Never writes SQL directly.
 */
package com.example.service;

import com.example.dto.TransactionRequest;
import com.example.dto.TransactionResponse;
import com.example.entity.Transaction;
import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.event.TransactionCreatedEvent;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, TransactionSource source) {
        Transaction tx = Transaction.builder()
                .accountId(request.getAccountId())
                .payeeId(request.getPayeeId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .type(request.getType())
                .timestamp(Instant.now())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .source(source)
                .build();

        Transaction saved = transactionRepository.save(tx);
        log.debug("Transaction saved: {} [{}]", saved.getTransactionId(), source);

        eventPublisher.publishEvent(new TransactionCreatedEvent(this, saved));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionId(tx.getTransactionId())
                .accountId(tx.getAccountId())
                .payeeId(tx.getPayeeId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .type(tx.getType())
                .timestamp(tx.getTimestamp())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .source(tx.getSource())
                .build();
    }
}
