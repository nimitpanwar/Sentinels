package com.example.repository;

import com.example.entity.InvestigationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestigationMessageRepository extends JpaRepository<InvestigationMessage, Integer> {

    List<InvestigationMessage> findByAlertAlertIdOrderByCreatedAtDesc(Integer alertId);
}
