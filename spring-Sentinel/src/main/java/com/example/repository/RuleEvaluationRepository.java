package com.example.repository;

import com.example.entity.RuleEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluation, Integer> {

    List<RuleEvaluation> findByTransactionTransactionId(Integer transactionId);
}
