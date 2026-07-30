package com.example.repository;

import com.example.entity.TransactionQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionQueueStatusRepository extends JpaRepository<TransactionQueueStatus, Integer> {
}
