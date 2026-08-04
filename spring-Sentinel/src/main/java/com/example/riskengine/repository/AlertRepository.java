package com.example.riskengine.repository;

import com.example.riskengine.model.Alert;
import com.example.riskengine.model.AlertStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * DEAD CODE: superseded by com.example.repository.AlertRepository (real JPA
 * repository backing the schema's 'alerts' table, used by AlertManager).
 * No longer a Spring bean (@Repository removed) so it is never instantiated
 * and does not collide with the new AlertRepository bean name.
 */
public class AlertRepository {

    private final AlertJpaRepository jpaRepository;

    public AlertRepository(AlertJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public String nextAlertId() {
        return "ALERT-" + (jpaRepository.count() + 101);
    }

    public void save(Alert alert) {
        jpaRepository.save(toEntity(alert));
    }

    public Optional<Alert> findById(String alertId) {
        return jpaRepository.findById(alertId).map(this::toDomain);
    }

    /** Finds an active alert for the same account, used for merge/dedup decisions. */
    public Optional<Alert> findActiveAlertForAccount(String accountId) {
        return jpaRepository.findByAccountId(accountId).stream()
                .map(this::toDomain)
            .filter(a -> a.isActive())
                .findFirst();
    }

    public Collection<Alert> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private AlertEntity toEntity(Alert alert) {
        return AlertEntity.builder()
                .id(alert.getId())
                .accountId(alert.getAccountId())
                .payeeId(alert.getPayeeId())
                .transactionIds(alert.getTransactionIds())
                .riskScore(alert.getRiskScore())
                .severity(alert.getSeverity())
                .triggeredRules(alert.getTriggeredRules())
                .evidence(alert.getEvidence())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .resolutionNotes(alert.getResolutionNotes())
                .build();
    }

    private Alert toDomain(AlertEntity entity) {
        Alert alert = new Alert(entity.getId(), entity.getAccountId(), entity.getPayeeId(), entity.getCreatedAt());
        alert.getTransactionIds().addAll(entity.getTransactionIds());
        alert.setRiskScore(entity.getRiskScore());
        alert.setSeverity(entity.getSeverity());
        alert.getTriggeredRules().addAll(entity.getTriggeredRules());
        alert.getEvidence().addAll(entity.getEvidence());
        alert.setStatus(entity.getStatus() != null ? entity.getStatus() : AlertStatus.OPEN);
        alert.setUpdatedAt(entity.getUpdatedAt());
        alert.setResolutionNotes(entity.getResolutionNotes());
        return alert;
    }
}
