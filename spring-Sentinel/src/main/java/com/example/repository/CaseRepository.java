package com.example.repository;

import com.example.entity.Case;
import com.example.enums.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

    List<Case> findByAccountAccountIdAndStatusNotOrderByCreatedAtDesc(Integer accountId, CaseStatus status);
}
