package com.example.repository;

import com.example.entity.InvestigationResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestigationResponseRepository extends JpaRepository<InvestigationResponse, Integer> {

    Optional<InvestigationResponse> findByMessageMessageId(Integer messageId);

    Optional<InvestigationResponse> findByMessageResponseToken(String responseToken);

    List<InvestigationResponse> findByMessageAlertAlertIdOrderBySubmittedAtDesc(Integer alertId);
}