package com.example.repository;

import com.example.entity.Alert;
import com.example.enums.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findFirstByACaseCaseIdOrderByCreatedAtDesc(Integer caseId);

    Optional<Alert> findByTransactionTransactionId(Integer transactionId);

    List<Alert> findByStatusOrderByCreatedAtDesc(CaseStatus status);

    List<Alert> findByACaseCaseIdOrderByCreatedAtDesc(Integer caseId);
    /** Keeps every Alert linked to a Case in sync when the Case's lifecycle status changes (see AlertManager). */
    @Modifying(clearAutomatically = true)
    @Query("update Alert a set a.status = :status where a.aCase.caseId = :caseId")
    int updateStatusByCaseId(@Param("caseId") Integer caseId, @Param("status") CaseStatus status);
}
