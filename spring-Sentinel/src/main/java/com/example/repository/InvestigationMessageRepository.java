package com.example.repository;

import com.example.entity.InvestigationMessage;
import com.example.enums.InvestigationMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestigationMessageRepository extends JpaRepository<InvestigationMessage, Integer> {

    List<InvestigationMessage> findByAlertAlertIdOrderByCreatedAtDesc(Integer alertId);

    Optional<InvestigationMessage> findByResponseToken(String responseToken);

    Optional<InvestigationMessage> findTopByAlertAlertIdOrderByCreatedAtDesc(Integer alertId);

    boolean existsByACaseCaseId(Integer caseId);

    boolean existsByACaseCaseIdAndDeliveryStatus(Integer caseId, InvestigationMessageStatus deliveryStatus);
}
