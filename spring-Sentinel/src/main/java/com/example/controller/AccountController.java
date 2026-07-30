package com.example.controller;

import com.example.dto.AccountRequest;
import com.example.entity.Account;
import com.example.entity.Customer;
import com.example.repository.AccountRepository;
import com.example.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD endpoints for accounts - lets Postman testers create real rows to reference from transactions. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountController(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody AccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + request.getCustomerId()));

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountType(request.getAccountType());
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(accountRepository.save(account));
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAll() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getById(@PathVariable Integer id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
