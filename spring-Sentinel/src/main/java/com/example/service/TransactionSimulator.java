/**
 * TransactionSimulator
 * 
 * PURPOSE: A background data generator that creates fake transactions automatically
 *          on a schedule. This is used for testing, demoing, and generating datasets
 *          for the Rule Engine to evaluate.
 * 
 * HOW IT WORKS:
 *   - Runs on a @Scheduled timer (every 3 seconds, configurable)
 *   - Each tick either generates a random transaction OR triggers a scenario
 *   - 10% of the time (configurable), it triggers a special scenario instead
 *   - All transactions go through TransactionService (same path as API)
 * 
 * SCENARIO TYPES (triggered 10% of the time):
 *   1. VELOCITY: Creates 6 rapid transactions from the same account to same payee
 *      (designed to trigger the Velocity rule)
 *   2. HIGH-VALUE: Creates a single large transaction ($9k–$14k)
 *      (designed to trigger the High-Value rule)
 *   3. NEW-PAYEE: Creates a transaction to a payee ID never seen before
 *      (designed to trigger the New-Payee rule)
 * 
 * RANDOM BASELINE (90% of the time):
 *   - Picks random account, payee, amount, type, description
 *   - Amount distribution: 70% small ($5–$500), 25% medium ($500–$2k), 5% large ($2k–$8k)
 *   - Type: Random DEBIT or CREDIT
 * 
 * CONTROL: Can be started/stopped via SimulatorController endpoints.
 *          Check if running via isRunning() method.
 * 
 * CONFIGURATION: All settings come from application.properties:
 *   - simulator.enabled: Start on app boot?
 *   - simulator.interval-ms: How often to generate
 *   - simulator.scenario-probability: % chance of scenario vs random
 */
package com.example.service;

import com.example.dto.TransactionRequest;
import com.example.entity.Payee;
import com.example.enums.TransactionSource;
import com.example.enums.TransactionType;
import com.example.repository.AccountRepository;
import com.example.repository.PayeeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

// NOTE: Lombok (@Slf4j/@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
@Service
public class TransactionSimulator {

    private static final Logger log = LoggerFactory.getLogger(TransactionSimulator.class);

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final PayeeRepository payeeRepository;

    public TransactionSimulator(TransactionService transactionService, AccountRepository accountRepository,
                                 PayeeRepository payeeRepository) {
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
    }

    @Value("${simulator.enabled:true}")
    private boolean enabledByDefault;

    @Value("${simulator.scenario-probability:0.1}")
    private double scenarioProbability;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random random = new Random();

    // Real account/payee IDs are seeded into the DB by SeedDataService - loaded
    // lazily (on first tick) rather than at @PostConstruct, since bean
    // initialization order between SeedDataService and this class isn't guaranteed.
    private List<Integer> accountPool = List.of();
    private List<Integer> payeePool = List.of();

    private static final List<String> DESCRIPTIONS = List.of(
            "Online purchase", "Wire transfer", "Bill payment", "Subscription charge",
            "ATM withdrawal", "POS payment", "Bank transfer", "Refund credit",
            "Utility payment", "Insurance premium"
    );

    @PostConstruct
    public void init() {
        running.set(enabledByDefault);
        log.info("TransactionSimulator initialized. Running: {}", running.get());
    }

    @Scheduled(fixedDelayString = "${simulator.interval-ms:3000}")
    public void generateTransaction() {
        if (!running.get()) return;
        if (!refreshPoolsIfNeeded()) return; // no seeded accounts/payees yet

        if (random.nextDouble() < scenarioProbability) {
            int scenario = random.nextInt(3);
            switch (scenario) {
                case 0 -> triggerVelocityScenario();
                case 1 -> triggerHighValueScenario();
                case 2 -> triggerNewPayeeScenario();
            }
        } else {
            generateRandomTransaction();
        }
    }

    /** @return true if the pools are populated and ready to use. */
    private boolean refreshPoolsIfNeeded() {
        if (accountPool.isEmpty()) {
            accountPool = new ArrayList<>(accountRepository.findAll().stream().map(com.example.entity.Account::getAccountId).toList());
        }
        if (payeePool.isEmpty()) {
            payeePool = new ArrayList<>(payeeRepository.findAll().stream().map(Payee::getPayeeId).toList());
        }
        return !accountPool.isEmpty() && !payeePool.isEmpty();
    }

    // ── Random baseline transaction ──────────────────────────────────────────

    private void generateRandomTransaction() {
        TransactionRequest req = buildRandomRequest(randomFrom(accountPool), randomFrom(payeePool));
        transactionService.createTransaction(req, TransactionSource.SIMULATOR);
    }

    // ── Demo scenarios ───────────────────────────────────────────────────────

    public void triggerVelocityScenario() {
        if (!refreshPoolsIfNeeded()) return;
        Integer account = randomFrom(accountPool);
        Integer payee   = randomFrom(payeePool);
        log.info("Simulator: velocity scenario for account={}", account);
        for (int i = 0; i < 6; i++) {
            TransactionRequest req = new TransactionRequest();
            req.setAccountId(account);
            req.setPayeeId(payee);
            req.setAmount(BigDecimal.valueOf(50 + random.nextInt(200)));
            req.setCurrency("USD");
            req.setType(TransactionType.DEBIT);
            req.setDescription("Velocity test tx-" + (i + 1));
            transactionService.createTransaction(req, TransactionSource.SIMULATOR);
        }
    }

    public void triggerHighValueScenario() {
        if (!refreshPoolsIfNeeded()) return;
        Integer account = randomFrom(accountPool);
        BigDecimal amount = BigDecimal.valueOf(9000 + random.nextInt(5000));
        log.info("Simulator: high-value scenario for account={} amount={}", account, amount);
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setPayeeId(randomFrom(payeePool));
        req.setAmount(amount);
        req.setCurrency("USD");
        req.setType(TransactionType.DEBIT);
        req.setDescription("High-value transfer");
        transactionService.createTransaction(req, TransactionSource.SIMULATOR);
    }

    public void triggerNewPayeeScenario() {
        if (!refreshPoolsIfNeeded()) return;
        Integer account = randomFrom(accountPool);
        Payee newPayee = new Payee();
        newPayee.setPayeeName("Simulator-generated payee");
        newPayee.setPayeeIdentifier("SIM-" + System.currentTimeMillis());
        newPayee = payeeRepository.save(newPayee);
        log.info("Simulator: new-payee scenario for account={} payee={}", account, newPayee.getPayeeId());
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setPayeeId(newPayee.getPayeeId());
        req.setAmount(BigDecimal.valueOf(100 + random.nextInt(500)));
        req.setCurrency("USD");
        req.setType(TransactionType.DEBIT);
        req.setDescription("First-time payee transaction");
        transactionService.createTransaction(req, TransactionSource.SIMULATOR);
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    public void start() {
        running.set(true);
        log.info("Simulator started");
    }

    public void stop() {
        running.set(false);
        log.info("Simulator stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TransactionRequest buildRandomRequest(Integer accountId, Integer payeeId) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(accountId);
        req.setPayeeId(payeeId);
        req.setCurrency("USD");
        req.setType(random.nextBoolean() ? TransactionType.DEBIT : TransactionType.CREDIT);
        req.setDescription(randomFrom(DESCRIPTIONS));

        // Weighted distribution: 70% small, 25% medium, 5% large
        double roll = random.nextDouble();
        if (roll < 0.70) {
            req.setAmount(BigDecimal.valueOf(5 + random.nextInt(495)).setScale(2, RoundingMode.HALF_UP));
        } else if (roll < 0.95) {
            req.setAmount(BigDecimal.valueOf(500 + random.nextInt(1500)).setScale(2, RoundingMode.HALF_UP));
        } else {
            req.setAmount(BigDecimal.valueOf(2000 + random.nextInt(6000)).setScale(2, RoundingMode.HALF_UP));
        }

        return req;
    }

    private <T> T randomFrom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}

