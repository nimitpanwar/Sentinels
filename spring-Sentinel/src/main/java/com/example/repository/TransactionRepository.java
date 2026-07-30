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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAccountAccountIdAndTransactionTimestampBetween(Integer accountId, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByAccountAccountIdAndPayeePayeeId(Integer accountId, Integer payeeId);

    List<Transaction> findByAccountAccountIdOrderByTransactionTimestampDesc(Integer accountId);
}

