package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One account's network-risk score from a single run - the row shape for
 * GET /api/network/scores and the per-account timeline. evidence is the raw
 * signals JSON (see entity.AccountNetworkScore.evidenceJson) passed through
 * as-is so the frontend can render a breakdown without Spring needing to
 * understand each individual signal.
 */
public record NetworkScoreResponse(
        Integer runId,
        Integer accountId,
        String accountNumber,
        BigDecimal networkRiskScore,
        BigDecimal pageRankPercentile,
        Integer sharedPayeeCount,
        Integer communityId,
        Integer communitySize,
        BigDecimal growthScore,
        BigDecimal fraudExposureScore,
        String evidence,
        String networkReason,
        LocalDateTime computedAt
) {
}
