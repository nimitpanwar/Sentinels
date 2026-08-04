package com.example.dto;

import java.util.List;

/** Full detail payload for GET /api/network/accounts/{id}: latest score + evidence + score-over-time. */
public record NetworkAccountDetailResponse(
        NetworkScoreResponse latest,
        List<NetworkTimelinePoint> timeline
) {
}
