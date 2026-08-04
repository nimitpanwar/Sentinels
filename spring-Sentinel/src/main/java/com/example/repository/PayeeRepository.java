package com.example.repository;

import com.example.entity.Payee;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayeeRepository extends JpaRepository<Payee, Integer> {

    // Same rationale as AccountRepository.findById - payees are reference
    // data looked up on every transaction creation, rarely change.
    @Cacheable("payees")
    @Override
    Optional<Payee> findById(Integer id);
}
