package com.example.riskengine.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * The customer's historical behaviour profile, computed from real
 * transaction history via HistoricalProfileService (DB-backed).
 * Adapted from backend/'s com.frauddetection.model.HistoricalProfile -
 * knownDevices removed, deviceId/DeviceChangeRule no longer exist.
 * accountId/knownPayees use Integer IDs to match the real relational schema.
 */
public class HistoricalProfile {

    private final Integer accountId;
    private final BigDecimal meanAmount;
    private final BigDecimal stdDevAmount;
    private final double historicalAvgVelocityPerWindow;
    private final Set<Integer> knownPayees;
    private final Set<String> knownLocations;
    private final Set<String> knownMerchantCategories;
    private final int normalStartHour; // inclusive, 24h clock
    private final int normalEndHour;   // exclusive, 24h clock

    public HistoricalProfile(Integer accountId, BigDecimal meanAmount, BigDecimal stdDevAmount,
                              double historicalAvgVelocityPerWindow, Set<Integer> knownPayees,
                              Set<String> knownLocations, Set<String> knownMerchantCategories,
                              int normalStartHour, int normalEndHour) {
        this.accountId = accountId;
        this.meanAmount = meanAmount;
        this.stdDevAmount = stdDevAmount;
        this.historicalAvgVelocityPerWindow = historicalAvgVelocityPerWindow;
        this.knownPayees = knownPayees;
        this.knownLocations = knownLocations;
        this.knownMerchantCategories = knownMerchantCategories;
        this.normalStartHour = normalStartHour;
        this.normalEndHour = normalEndHour;
    }

    public Integer getAccountId() { return accountId; }
    public BigDecimal getMeanAmount() { return meanAmount; }
    public BigDecimal getStdDevAmount() { return stdDevAmount; }
    public double getHistoricalAvgVelocityPerWindow() { return historicalAvgVelocityPerWindow; }
    public Set<Integer> getKnownPayees() { return knownPayees; }
    public Set<String> getKnownLocations() { return knownLocations; }
    public Set<String> getKnownMerchantCategories() { return knownMerchantCategories; }
    public int getNormalStartHour() { return normalStartHour; }
    public int getNormalEndHour() { return normalEndHour; }
}

