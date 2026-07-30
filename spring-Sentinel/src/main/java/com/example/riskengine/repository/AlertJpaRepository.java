package com.example.riskengine.repository;

import java.util.List;
import java.util.Optional;

/**
 * DEAD CODE: superseded by com.example.repository.AlertRepository (real JPA
 * repository backing the schema's 'alerts' table). No longer a Spring Data
 * interface (extends nothing Spring recognizes) so it is never instantiated
 * and does not collide with the new AlertRepository bean. Method stubs kept
 * only so the dead AlertRepository adapter class above still compiles.
 */
public interface AlertJpaRepository {

    List<AlertEntity> findByAccountId(String accountId);
    AlertEntity save(AlertEntity entity);
    Optional<AlertEntity> findById(String id);
    List<AlertEntity> findAll();
    long count();
}

