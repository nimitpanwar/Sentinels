/**
 * TransactionRepository Interface
 * 
 * PURPOSE: The ONLY class allowed to directly talk to the database for transactions.
 *          Acts as a bridge between our application and the MySQL 'transactions' table.
 * 
 * WHAT IT DOES:
 *   - Provides basic CRUD operations automatically (save, find, delete, etc.)
 *   - Declares custom query methods that the Rule Engine will use:
 *     * findByAccountIdAndTransactionTimestampBetween: Find all transactions from an
 *       account within a specific time period (used for Velocity rule)
 *     * findByAccountIdAndPayeeId: Find transactions between an account and payee
 *       (used for New Payee rule)
 *     * findByAccountIdOrderByTransactionTimestampDesc: Get recent transactions for
 *       an account ordered from newest to oldest
 * 
 * WHY SEPARATE: Keeps all database access in one place, making it easy to
 *               change queries or switch databases without touching service code.
 */
package com.example.repository;

import com.example.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByAccountAccountIdAndTransactionTimestampBetween(Integer accountId, LocalDateTime from, LocalDateTime to);

    /** COUNT-only variant - avoids loading full rows (with their EAGER account/payee joins) just to count them. */
    long countByAccountAccountIdAndTransactionTimestampBetween(Integer accountId, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByAccountAccountIdAndPayeePayeeId(Integer accountId, Integer payeeId);

    List<Transaction> findByAccountAccountIdOrderByTransactionTimestampDesc(Integer accountId);

    /**
     * Bipartite "shared payee" neighborhood for one account, projected onto
     * account-account edges: any other account that transacted with at
     * least one of this account's payees within the lookback window, ranked
     * by how many distinct payees they have in common. Powers
     * GET /api/network/accounts/{id}/graph - a small (LIMIT-bounded)
     * subgraph, not the whole network, per the "keep the graph small /
     * investigators rarely need the whole network" design decision.
     */
    @Query(value = """
            SELECT t2.account_id AS neighborId, COUNT(DISTINCT t1.payee_id) AS sharedPayees
            FROM transactions t1
            JOIN transactions t2
              ON t2.payee_id = t1.payee_id
             AND t2.account_id <> t1.account_id
             AND t2.transaction_timestamp >= :since
            WHERE t1.account_id = :accountId
              AND t1.transaction_timestamp >= :since
            GROUP BY t2.account_id
            ORDER BY sharedPayees DESC
            """, nativeQuery = true)
    List<SharedPayeeNeighbor> findSharedPayeeNeighbors(
            @Param("accountId") Integer accountId,
            @Param("since") LocalDateTime since,
            Pageable limit);

    /** Projection for {@link #findSharedPayeeNeighbors}. */
    interface SharedPayeeNeighbor {
        Integer getNeighborId();
        Long getSharedPayees();
    }
}

