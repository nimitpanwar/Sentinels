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
 * EVENT PUBLISHING: After every save, it publishes TransactionCreatedEvent and
 *                   returns immediately - it does NOT wait for rule evaluation.
 *                   TransactionEventListener picks the event up on a background
 *                   thread (after this method's transaction commits) and performs
 *                   the actual scoring + alert creation there. This keeps
 *                   recording fast and independent of rule evaluation.
 * 
 * DATABASE ACCESS: Only talks to the database through TransactionRepository.
 *                  Never writes SQL directly.
 */
package com.example.service;

import com.example.dto.TransactionFilter;
import com.example.dto.TransactionRequest;
import com.example.dto.TransactionResponse;
import com.example.entity.Account;
import com.example.entity.Payee;
import com.example.entity.Transaction;
import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.entity.RuleEvaluation;
import com.example.event.TransactionCreatedEvent;
import com.example.repository.AccountRepository;
import com.example.repository.AlertRepository;
import com.example.repository.PayeeRepository;
import com.example.repository.RuleEvaluationRepository;
import com.example.repository.TransactionRepository;
import com.example.repository.TransactionSpecifications;
import com.example.riskengine.service.EvaluationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

// NOTE: Lombok (@Slf4j/@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PayeeRepository payeeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final AlertRepository alertRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
                               PayeeRepository payeeRepository, ApplicationEventPublisher eventPublisher,
                               RuleEvaluationRepository ruleEvaluationRepository, AlertRepository alertRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.eventPublisher = eventPublisher;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.alertRepository = alertRepository;
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
                .transactionTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .location(request.getLocation())
                .merchantCategory(request.getMerchantCategory())
                .build();

        Transaction saved = transactionRepository.save(tx);
        log.debug("Transaction saved: {} [{}]", saved.getTransactionId(), source);

        // Decoupled: recording stays fast/independent of rule evaluation.
        // Publish the event and return immediately - TransactionEventListener
        // picks it up asynchronously (only after this transaction commits)
        // and performs the actual scoring + alert creation on a background
        // thread. Callers won't see riskScore/alertId in this response; poll
        // GET /api/transactions/{id} or GET /api/alerts afterward.
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, saved));

        return toResponse(saved, null);
    }

    /**
     * Paginated, filterable transaction listing (Appendix C "Transactions
     * List Screen" - filter by date range/account/amount range, search by
     * ID or description, sort by any column via Pageable). Filters are
     * combined with AND; any null field in {@code filter} means "no
     * constraint on that field".
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilter filter, Pageable pageable) {
        Specification<Transaction> spec = Specification
                .where(TransactionSpecifications.hasAccountId(filter.accountId()))
                .and(TransactionSpecifications.hasPayeeId(filter.payeeId()))
                .and(TransactionSpecifications.hasStatus(filter.status()))
                .and(TransactionSpecifications.hasType(filter.type()))
                .and(TransactionSpecifications.amountAtLeast(filter.minAmount()))
                .and(TransactionSpecifications.amountAtMost(filter.maxAmount()))
                .and(TransactionSpecifications.timestampFrom(filter.from()))
                .and(TransactionSpecifications.timestampTo(filter.to()))
                .and(TransactionSpecifications.search(filter.search()));

        return transactionRepository.findAll(spec, pageable)
                .map(this::toResponseWithStoredEvaluation);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(this::toResponseWithStoredEvaluation)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    /**
     * Builds the response for an already-recorded transaction by looking up
     * whatever the async TransactionEventListener has (or hasn't yet) written
     * to the DB - rule_evaluations always get logged (even for non-triggering
     * rules), alerts only exist if the risk score crossed the alert threshold.
     * Previously this always passed a null EvaluationOutcome here, so GET
     * responses NEVER showed the risk score/alert/case even long after async
     * evaluation had completed - that was the bug.
     */
    private TransactionResponse toResponseWithStoredEvaluation(Transaction tx) {
        List<RuleEvaluation> evaluations = ruleEvaluationRepository.findByTransactionTransactionId(tx.getTransactionId());
        if (evaluations.isEmpty()) {
            // Async evaluation hasn't run yet (or produced nothing) - not a bug,
            // just means the background listener hasn't picked it up yet.
            return toResponse(tx, null);
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        List<String> triggeredRuleNames = evaluations.stream()
                .filter(RuleEvaluation::isTriggered)
                .map(e -> e.getRule().getRuleType().name())
                .toList();
        List<String> evidence = evaluations.stream()
                .filter(RuleEvaluation::isTriggered)
                .map(RuleEvaluation::getReason)
                .toList();
        for (RuleEvaluation e : evaluations) {
            if (e.isTriggered()) {
                double weight = e.getRule().getWeight().doubleValue();
                weightedSum += e.getRiskScore().doubleValue() * weight;
                totalWeight += weight;
            }
        }
        int riskScore = totalWeight == 0 ? 0 : (int) Math.round((weightedSum / totalWeight) * 100);

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
                .merchantCategory(tx.getMerchantCategory())
                .riskScore(riskScore)
                .triggeredRules(triggeredRuleNames)
                .evidence(evidence);

        alertRepository.findByTransactionTransactionId(tx.getTransactionId()).ifPresent(alert -> {
            builder.alertId(alert.getAlertId())
                    .alertSeverity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                    .alertStatus(alert.getStatus() != null ? alert.getStatus().name() : null);

            if (alert.getCase() != null) {
                builder.caseId(alert.getCase().getCaseId())
                        .caseSeverity(alert.getCase().getSeverity() != null ? alert.getCase().getSeverity().name() : null)
                        .caseStatus(alert.getCase().getStatus() != null ? alert.getCase().getStatus().name() : null);
            }
        });

        return builder.build();
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


