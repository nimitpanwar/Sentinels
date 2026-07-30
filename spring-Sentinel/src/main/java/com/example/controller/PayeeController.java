package com.example.controller;

import com.example.dto.PayeeRequest;
import com.example.entity.Payee;
import com.example.repository.PayeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD endpoints for payees - lets Postman testers create real rows to reference from transactions. */
@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeRepository payeeRepository;

    public PayeeController(PayeeRepository payeeRepository) {
        this.payeeRepository = payeeRepository;
    }

    @PostMapping
    public ResponseEntity<Payee> create(@RequestBody PayeeRequest request) {
        Payee payee = new Payee();
        payee.setPayeeName(request.getPayeeName());
        payee.setPayeeIdentifier(request.getPayeeIdentifier());
        return ResponseEntity.status(HttpStatus.CREATED).body(payeeRepository.save(payee));
    }

    @GetMapping
    public ResponseEntity<List<Payee>> getAll() {
        return ResponseEntity.ok(payeeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payee> getById(@PathVariable Integer id) {
        return payeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
