package com.example.repository;

import com.example.entity.Case;
import com.example.enums.CaseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Integer> {

    List<Case> findByAccountAccountIdAndStatusNotOrderByCreatedAtDesc(Integer accountId, CaseStatus status);

    /**
     * Same lookup as above, but with a PESSIMISTIC_WRITE row lock. Must be
     * called inside a transaction (see AlertManager.process) so the lock is
     * held from this read through the eventual case insert/update, which
     * serializes concurrent evaluations for the same account and prevents
     * two of them from both seeing "no open case" and each creating a
     * duplicate one.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Case c where c.account.accountId = :accountId and c.status not in :excludedStatuses order by c.createdAt desc")
    List<Case> findByAccountForUpdate(@Param("accountId") Integer accountId, @Param("excludedStatuses") Collection<CaseStatus> excludedStatuses);

    // ---- Aggregate queries for the stats endpoint (see CaseController#stats) ----

    @Query("select c.status, count(c) from Case c group by c.status")
    List<Object[]> countGroupedByStatus();

    @Query("select c.createdAt, c.acknowledgedAt from Case c where c.acknowledgedAt is not null")
    List<Object[]> findCreatedAndAcknowledgedTimestamps();

    @Query("select c.createdAt, c.closedAt from Case c where c.closedAt is not null")
    List<Object[]> findCreatedAndClosedTimestamps();
}
