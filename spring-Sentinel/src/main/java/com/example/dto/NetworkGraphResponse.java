package com.example.dto;

import java.util.List;

/**
 * Small (LIMIT-bounded) neighborhood subgraph for GET
 * /api/network/accounts/{id}/graph - deliberately scoped to one account's
 * immediate connections rather than the whole network (see
 * TransactionRepository.findSharedPayeeNeighbors). Edges are a bipartite
 * PROJECTION (accounts connected via shared payees), not literal
 * account-to-account money transfers.
 */
public record NetworkGraphResponse(
        Integer centerAccountId,
        List<NetworkGraphNode> nodes,
        List<NetworkGraphEdge> edges
) {
    public record NetworkGraphNode(
            Integer accountId,
            String accountNumber,
            boolean isCenter,
            java.math.BigDecimal networkRiskScore
    ) {
    }

    public record NetworkGraphEdge(
            Integer sourceAccountId,
            Integer targetAccountId,
            long sharedPayeeCount
    ) {
    }
}
