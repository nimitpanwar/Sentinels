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
