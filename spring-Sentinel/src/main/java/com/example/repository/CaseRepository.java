package com.example.repository;

import com.example.entity.Case;
import com.example.enums.CaseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
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
    @Query("select c from Case c where c.account.accountId = :accountId and c.status <> :excludedStatus order by c.createdAt desc")
    List<Case> findByAccountForUpdate(@Param("accountId") Integer accountId, @Param("excludedStatus") CaseStatus excludedStatus);
}
