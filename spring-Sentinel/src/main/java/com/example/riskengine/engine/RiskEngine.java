package com.example.riskengine.engine;

import com.example.entity.Rule;
import com.example.entity.RuleEvaluation;
import com.example.entity.Transaction;
import com.example.repository.RuleEvaluationRepository;
import com.example.repository.RuleRepository;
import com.example.riskengine.model.HistoricalProfile;
import com.example.riskengine.model.RiskResult;
import com.example.riskengine.model.RuleResult;
import com.example.riskengine.rules.RiskRule;
import com.example.riskengine.service.HistoricalProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Risk Engine.
 *
 * Responsibility (Separation of Risk Engine and Alert Management): evaluate
 * each transaction against ALL active DB-configured rules and produce a
 * risk score (0-100) plus the rules that were triggered. The Risk Engine has
 * NO knowledge of alerts/cases - that decision belongs entirely to AlertManager.
 *
 * Ported from backend/'s com.frauddetection.engine.RiskEngine. Adapted to:
 *  - use HistoricalProfileService (DB-backed) instead of HistoricalDataStore
 *  - read enabled/weight/threshold from the DB-backed 'rules' table
 *    (RuleRepository) instead of a hardcoded RuleConfig
 *  - log one RuleEvaluation audit row per active rule per transaction
 *  - operate on LocalDateTime directly (the real schema's transaction_timestamp
 *    column), no more Instant/LocalDateTime conversion juggling
 */
@Service
public class RiskEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskEngine.class);

    private final Map<com.example.enums.RuleType, RiskRule> rulesByType;
    private final RuleRepository ruleRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final HistoricalProfileService profileService;

    public RiskEngine(List<RiskRule> rules, RuleRepository ruleRepository,
                       RuleEvaluationRepository ruleEvaluationRepository, HistoricalProfileService profileService) {
        this.rulesByType = new EnumMap<>(com.example.enums.RuleType.class);
        for (RiskRule rule : rules) {
            this.rulesByType.put(rule.getRuleType(), rule);
        }
        this.ruleRepository = ruleRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.profileService = profileService;
    }

    public RiskResult evaluate(Transaction transaction) {
        HistoricalProfile profile = profileService.getProfile(transaction);

        List<Rule> activeRules = ruleRepository.findByActiveTrue();

        List<RuleResult> results = new ArrayList<>();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Rule rule : activeRules) {
            RiskRule ruleImpl = rulesByType.get(rule.getRuleType());
            if (ruleImpl == null) {
                // e.g. DEVICE_CHANGE - present in the schema/enum, but no Java implementation is bound.
                continue;
            }

            RuleResult result;
            try {
                result = ruleImpl.evaluate(transaction, profile, rule);
            } catch (Exception ex) {
                // Graceful Handling of Missing Data: a single rule failing
                // (e.g. an unexpected null field) must never break the
                // whole evaluation - fail safe with zero contribution.
                log.warn("Rule {} failed for transaction {}: {}", rule.getRuleType(), transaction.getTransactionId(), ex.getMessage());
                result = new RuleResult(rule.getRuleType().name(), false, 0.0,
                        "Rule skipped due to an unexpected error: " + ex.getMessage());
            }

            results.add(result);
            logEvaluation(transaction, rule, result);

            // Only rules that actually TRIGGERED contribute to the final score.
            if (result.isTriggered()) {
                double weight = rule.getWeight().doubleValue();
                weightedSum += result.getScore() * weight;
                totalWeight += weight;
            }
        }

        int finalScore = totalWeight == 0
                ? 0
                : (int) Math.round((weightedSum / totalWeight) * 100);

        return new RiskResult(transaction.getTransactionId(), transaction.getAccountId(), transaction.getPayeeId(),
                finalScore, results, transaction.getTransactionTimestamp());
    }

    private void logEvaluation(Transaction transaction, Rule rule, RuleResult result) {
        RuleEvaluation evaluation = new RuleEvaluation();
        evaluation.setTransaction(transaction);
        evaluation.setRule(rule);
        evaluation.setRiskScore(BigDecimal.valueOf(result.getScore()).setScale(3, RoundingMode.HALF_UP));
        evaluation.setTriggered(result.isTriggered());
        evaluation.setReason(result.getReason());
        ruleEvaluationRepository.save(evaluation);
    }
}

