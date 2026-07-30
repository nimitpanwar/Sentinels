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
import com.example.entity.Account;
import com.example.entity.Payee;
import com.example.entity.Transaction;
import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.repository.AccountRepository;
import com.example.repository.PayeeRepository;
import com.example.repository.TransactionRepository;
import com.example.riskengine.service.EvaluationOutcome;
import com.example.riskengine.service.RiskEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// NOTE: Lombok (@Slf4j/@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PayeeRepository payeeRepository;
    private final RiskEvaluationService riskEvaluationService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
                               PayeeRepository payeeRepository, RiskEvaluationService riskEvaluationService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.riskEvaluationService = riskEvaluationService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, TransactionSource source) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));
        Payee payee = payeeRepository.findById(request.getPayeeId())
                .orElseThrow(() -> new RuntimeException("Payee not found: " + request.getPayeeId()));

        Transaction tx = Transaction.builder()
                .account(account)
                .payee(payee)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .type(request.getType())
                .transactionTimestamp(LocalDateTime.now())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .location(request.getLocation())
                .merchantCategory(request.getMerchantCategory())
                .build();

        Transaction saved = transactionRepository.save(tx);
        log.debug("Transaction saved: {} [{}]", saved.getTransactionId(), source);

        // Synchronous: score + alert-check happen in the same request/response
        // cycle, so the caller sees the final risk outcome immediately.
        EvaluationOutcome outcome = riskEvaluationService.evaluate(saved);

        return toResponse(saved, outcome);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(tx -> toResponse(tx, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(tx -> toResponse(tx, null))
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    private TransactionResponse toResponse(Transaction tx, EvaluationOutcome outcome) {
        TransactionResponse.Builder builder = TransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .accountId(tx.getAccountId())
                .payeeId(tx.getPayeeId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .type(tx.getType())
                .transactionTimestamp(tx.getTransactionTimestamp())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .location(tx.getLocation())
                .merchantCategory(tx.getMerchantCategory());

        if (outcome != null) {
            builder.riskScore(outcome.getRiskResult().getRiskScore())
                    .triggeredRules(outcome.getRiskResult().getTriggeredRules().stream()
                            .map(r -> r.getRuleName()).toList())
                    .evidence(outcome.getRiskResult().getTriggeredRules().stream()
                            .map(r -> r.getReason()).toList());

            outcome.getAlert().ifPresent(alert -> {
                builder.alertId(alert.getAlertId())
                        .alertSeverity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                        .alertStatus(alert.getStatus() != null ? alert.getStatus().name() : null);

                if (alert.getCase() != null) {
                    builder.caseId(alert.getCase().getCaseId())
                            .caseSeverity(alert.getCase().getSeverity() != null ? alert.getCase().getSeverity().name() : null)
                            .caseStatus(alert.getCase().getStatus() != null ? alert.getCase().getStatus().name() : null);
                }
            });
        }

        return builder.build();
    }
}


