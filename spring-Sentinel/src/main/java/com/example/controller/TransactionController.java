/**
 * TransactionController
 * 
 * PURPOSE: REST API endpoints for manual transaction operations. This is what
 *          external systems or clients use to create and retrieve transactions.
 * 
 * ENDPOINTS:
 *   - POST /api/transactions: Create a new transaction
 *     Input: TransactionRequest (accountId, payeeId, amount, etc.)
 *     Output: TransactionResponse (includes generated ID, timestamp, status)
 *   
 *   - GET /api/transactions: Retrieve all transactions
 *     Output: List of TransactionResponse objects
 *   
 *   - GET /api/transactions/{id}: Retrieve a single transaction by ID
 *     Output: Single TransactionResponse object
 * 
 * HOW IT WORKS:
 *   1. Receives HTTP request from client
 *   2. Maps request body to TransactionRequest DTO
 *   3. Calls TransactionService.createTransaction() with source=API
 *   4. Returns JSON response with HTTP status code
 * 
 * KEY POINT: All incoming transactions are marked with source=API to distinguish
 *            them from simulator-generated data (source=SIMULATOR).
 */
package com.example.controller;

import com.example.dto.TransactionRequest;
import com.example.dto.TransactionResponse;
import com.example.enums.TransactionSource;
import com.example.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request, TransactionSource.API);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
