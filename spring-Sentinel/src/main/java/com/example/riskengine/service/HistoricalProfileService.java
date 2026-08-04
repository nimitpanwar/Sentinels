package com.example.riskengine.service;

import com.example.entity.Transaction;
import com.example.repository.TransactionRepository;
import com.example.riskengine.model.HistoricalProfile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DB-backed replacement for backend/'s hardcoded HistoricalDataStore.
 * Builds a HistoricalProfile from real transaction history via
 * TransactionRepository instead of an in-memory map.
 */
@Service
public class HistoricalProfileService {

    /** Default lookback used to build the shared behavioural profile (mean/stddev/known sets). */
    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final TransactionRepository transactionRepository;

    public HistoricalProfileService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Builds the historical profile for the account behind {@code current},
     * excluding {@code current} itself (it has already been saved to the DB
     * by the time evaluation runs, and must not bias its own baseline).
     *
     * Bounded to the last {@link #DEFAULT_LOOKBACK_DAYS} days (previously this
     * fetched EVERY transaction the account ever made, unbounded - a growing
     * perf problem as history accumulates, and inconsistent with the lookback
     * this class already claimed to use).
     *
     * Cached per account for a short TTL (see CacheConfig) - this baseline is
     * a slow-moving statistical aggregate, not a hard threshold, so a few
     * seconds of staleness under a burst of transactions for the same account
     * is an acceptable trade-off for not re-scanning up to 90 days of rows on
     * every single evaluation.
     */
    @Cacheable(value = "historicalProfile", key = "#current.accountId")
    public HistoricalProfile getProfile(Transaction current) {
        LocalDateTime windowEnd = current.getTransactionTimestamp() != null ? current.getTransactionTimestamp() : LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime windowStart = windowEnd.minusDays(DEFAULT_LOOKBACK_DAYS);

        List<Transaction> history = transactionRepository
                .findByAccountAccountIdAndTransactionTimestampBetween(current.getAccountId(), windowStart, windowEnd)
                .stream()
                .filter(t -> !t.getTransactionId().equals(current.getTransactionId()))
                .toList();

        if (history.isEmpty()) {
            return coldStartProfile(current.getAccountId());
        }

        List<BigDecimal> amounts = history.stream().map(t -> t.getAmount()).toList();
        BigDecimal mean = average(amounts);
        BigDecimal stdDev = stdDeviation(amounts, mean);

        Set<Integer> knownPayees = new HashSet<>();
        Set<String> knownLocations = new HashSet<>();
        Set<String> knownCategories = new HashSet<>();
        int minHour = 23;
        int maxHour = 0;

        for (Transaction t : history) {
            knownPayees.add(t.getPayeeId());
            if (t.getLocation() != null && !t.getLocation().isBlank()) {
                knownLocations.add(t.getLocation());
            }
            if (t.getMerchantCategory() != null && !t.getMerchantCategory().isBlank()) {
                knownCategories.add(t.getMerchantCategory());
            }
            int hour = t.getTransactionTimestamp().getHour();
            minHour = Math.min(minHour, hour);
            maxHour = Math.max(maxHour, hour);
        }

        double avgVelocityPerWindow = history.size() / (double) DEFAULT_LOOKBACK_DAYS;

        return new HistoricalProfile(
                current.getAccountId(),
                mean,
                stdDev,
                avgVelocityPerWindow,
                knownPayees,
                knownLocations,
                knownCategories,
                minHour,
                Math.min(24, maxHour + 1)
        );
    }

    /**
     * Count of this account's transactions within the last {@code windowDays}
     * days. Deliberately NOT cached (unlike getProfile) - this is a live
     * threshold-style count, always computed via SQL COUNT(*), never stale.
     */
    public long getRecentTransactionCount(Integer accountId, LocalDateTime now, int windowDays) {
        LocalDateTime from = now.minusDays(windowDays);
        return transactionRepository.countByAccountAccountIdAndTransactionTimestampBetween(accountId, from, now);
    }

    private HistoricalProfile coldStartProfile(Integer accountId) {
        return new HistoricalProfile(
                accountId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0.0,
                Set.of(),
                Set.of(),
                Set.of(),
                0, 24
        );
    }

    private BigDecimal average(List<BigDecimal> amounts) {
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return sum.divide(BigDecimal.valueOf(amounts.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal stdDeviation(List<BigDecimal> amounts, BigDecimal mean) {
        if (amounts.size() < 2) {
            return BigDecimal.ZERO;
        }
        double meanValue = mean.doubleValue();
        double sumSquaredDiffs = amounts.stream()
                .mapToDouble(a -> Math.pow(a.doubleValue() - meanValue, 2))
                .sum();
        double variance = sumSquaredDiffs / amounts.size();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(4, RoundingMode.HALF_UP);
    }
}

