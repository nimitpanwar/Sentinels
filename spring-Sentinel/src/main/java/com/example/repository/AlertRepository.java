package com.example.repository;

import com.example.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findFirstByACaseCaseIdOrderByCreatedAtDesc(Integer caseId);

    Optional<Alert> findByTransactionTransactionId(Integer transactionId);

    List<Alert> findByACaseCaseIdOrderByCreatedAtDesc(Integer caseId);
}
