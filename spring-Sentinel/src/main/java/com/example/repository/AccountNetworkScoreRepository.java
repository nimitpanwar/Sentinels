package com.example.repository;

import com.example.entity.AccountNetworkScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountNetworkScoreRepository extends JpaRepository<AccountNetworkScore, Integer> {

    Page<AccountNetworkScore> findByRunIdAndNetworkRiskScoreGreaterThanEqualOrderByNetworkRiskScoreDesc(
            Integer runId, BigDecimal minScore, Pageable pageable);

    Page<AccountNetworkScore> findByRunIdOrderByNetworkRiskScoreDesc(Integer runId, Pageable pageable);

    Optional<AccountNetworkScore> findByRunIdAndAccountId(Integer runId, Integer accountId);

    /** Full score history for one account across every past run - powers the timeline view. */
    List<AccountNetworkScore> findByAccountIdOrderByComputedAtAsc(Integer accountId);

    long countByRunId(Integer runId);
}
