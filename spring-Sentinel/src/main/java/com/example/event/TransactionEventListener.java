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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class TransactionEventListener {

    @Value("${simulator.log-file:transactions_log.txt}")
    private String logFilePath;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    /**
     * Picks up TransactionCreatedEvent on a background thread.
     * Rule engine evaluation will be wired in here once the Rule Engine is built.
     */
    @Async
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();
        log.debug("TransactionEventListener received event for transaction: {}", tx.getTransactionId());

        // ── TEMP: File logger — replace this block with ruleEngineService.evaluate(tx) ──
        writeTransactionToFile(tx);
        // ── END TEMP ─────────────────────────────────────────────────────────────────────
    }

    private void writeTransactionToFile(Transaction tx) { //helper method, wont run once temp lines are deleted. can be left as is.
        String line = String.format("[%s] id=%-14s account=%-8s payee=%-10s amount=%10s %s type=%-6s status=%-9s source=%s%n",
                FORMATTER.format(tx.getTimestamp()),
                tx.getTransactionId(),
                tx.getAccountId(),
                tx.getPayeeId(),
                tx.getCurrency() + " " + tx.getAmount(),
                "",
                tx.getType(),
                tx.getStatus(),
                tx.getSource()
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(line);
        } catch (IOException e) {
            log.error("Failed to write transaction to log file: {}", e.getMessage());
        }
    }
}
