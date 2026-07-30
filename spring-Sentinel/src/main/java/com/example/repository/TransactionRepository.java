/**
 * TransactionRepository Interface
 * 
 * PURPOSE: The ONLY class allowed to directly talk to the database for transactions.
 *          Acts as a bridge between our application and the MySQL 'transactions' table.
 * 
 * WHAT IT DOES:
 *   - Provides basic CRUD operations automatically (save, find, delete, etc.)
 *   - Declares custom query methods that the Rule Engine will use:
 *     * findByAccountIdAndTimestampBetween: Find all transactions from an account
 *       within a specific time period (used for Velocity rule)
 *     * findByAccountIdAndPayeeId: Find transactions between an account and payee
 *       (used for New Payee rule)
 *     * findByAccountIdOrderByTimestampDesc: Get recent transactions for an account
 *       ordered from newest to oldest
 * 
 * WHY SEPARATE: Keeps all database access in one place, making it easy to
 *               change queries or switch databases without touching service code.
 */
package com.example.repository;

import com.example.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdAndTimestampBetween(String accountId, Instant from, Instant to);

    List<Transaction> findByAccountIdAndPayeeId(String accountId, String payeeId);

    List<Transaction> findByAccountIdOrderByTimestampDesc(String accountId);
}
