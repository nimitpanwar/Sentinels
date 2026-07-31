package com.example.repository;

import com.example.entity.Account;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    List<Account> findByCustomerCustomerId(Integer customerId);

    // Accounts are reference data looked up on every single transaction
    // creation (TransactionService.createTransaction) but rarely change -
    // caching removes a DB round trip from the hot ingestion path.
    @Cacheable("accounts")
    @Override
    Optional<Account> findById(Integer id);
}
