package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One point in an account's network-risk-score-over-time series (see GET /api/network/accounts/{id}). */
public record NetworkTimelinePoint(
        Integer runId,
        LocalDateTime computedAt,
        BigDecimal networkRiskScore
) {
}
