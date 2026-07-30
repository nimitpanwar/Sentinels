package com.example.riskengine.service;

import com.example.entity.Alert;
import com.example.entity.Transaction;
import com.example.entity.TransactionQueueStatus;
import com.example.enums.QueueStatus;
import com.example.repository.TransactionQueueStatusRepository;
import com.example.riskengine.alert.AlertManager;
import com.example.riskengine.engine.RiskEngine;
import com.example.riskengine.model.RiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrates a single transaction's full evaluation: RiskEngine scores it,
 * then AlertManager decides whether to create/merge/ignore an alert.
 * Called synchronously from TransactionService right after the transaction
 * is saved, so the HTTP response can include the result in one round trip.
 *
 * Also writes a TransactionQueueStatus audit row (PROCESSING -> EVALUATED/
 * FAILED) so the 'transaction_queue_status' table has a history, even
 * though evaluation itself is synchronous, not an actual async queue.
 */
@Service
public class RiskEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationService.class);

    private final RiskEngine riskEngine;
    private final AlertManager alertManager;
    private final TransactionQueueStatusRepository queueStatusRepository;

    public RiskEvaluationService(RiskEngine riskEngine, AlertManager alertManager,
                                  TransactionQueueStatusRepository queueStatusRepository) {
        this.riskEngine = riskEngine;
        this.alertManager = alertManager;
        this.queueStatusRepository = queueStatusRepository;
    }

    public EvaluationOutcome evaluate(Transaction transaction) {
        TransactionQueueStatus queueStatus = new TransactionQueueStatus();
        queueStatus.setTransaction(transaction);
        queueStatus.setQueueStatus(QueueStatus.PROCESSING);
        queueStatus.setPickedUpAt(LocalDateTime.now());
        queueStatus = queueStatusRepository.save(queueStatus);

        try {
            RiskResult riskResult = riskEngine.evaluate(transaction);
            Optional<Alert> alert = alertManager.process(riskResult, transaction);

            queueStatus.setQueueStatus(QueueStatus.EVALUATED);
            queueStatus.setEvaluatedAt(LocalDateTime.now());
            queueStatusRepository.save(queueStatus);

            return new EvaluationOutcome(riskResult, alert);
        } catch (RuntimeException ex) {
            log.error("Evaluation failed for transaction {}: {}", transaction.getTransactionId(), ex.getMessage(), ex);
            queueStatus.setQueueStatus(QueueStatus.FAILED);
            queueStatus.setRetryCount(queueStatus.getRetryCount() + 1);
            queueStatusRepository.save(queueStatus);
            throw ex;
        }
    }
}

