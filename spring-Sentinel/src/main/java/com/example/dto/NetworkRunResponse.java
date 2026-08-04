package com.example.dto;

import com.example.enums.NetworkRunStatus;
import com.example.enums.NetworkRunTrigger;

import java.time.LocalDateTime;

/** History-list row for GET /api/network/runs - lets the operator see freshness/staleness of the batch job. */
public record NetworkRunResponse(
        Integer runId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        NetworkRunStatus status,
        NetworkRunTrigger triggerType,
        Integer lookbackDays,
        String algorithmVersion,
        Integer accountsAnalyzed,
        Integer accountsFlagged,
        String errorMessage
) {
}
