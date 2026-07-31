package com.example.repository;

import com.example.entity.Rule;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Integer> {

    // Read on every single transaction evaluation; rules rarely change, so
    // this is cached for a short TTL (see CacheConfig). If/when rule edit
    // endpoints are added, they must evict the "activeRules" cache.
    @Cacheable("activeRules")
    List<Rule> findByActiveTrue();
}
