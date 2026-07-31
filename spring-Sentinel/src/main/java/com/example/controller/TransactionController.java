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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// NOTE: Lombok (@RequiredArgsConstructor) intentionally not used - see entity/Transaction.java note.
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request, TransactionSource.API);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns a paginated, newest-first list of transactions.
     *
     * Pagination params (all optional, Spring resolves from query string):
     *   ?page=0&size=50&sort=transactionTimestamp,desc
     *
     * Filter params (optional — accepted now, wiring to query deferred):
     *   ?status=COMPLETED  ?type=DEBIT  ?accountId=3  ?payeeId=7
     *   ?fromDate=2025-01-01T00:00:00  ?toDate=2025-12-31T23:59:59
     *   ?minAmount=100  ?maxAmount=5000
     *
     * Response shape (Spring Page wrapper):
     *   { "content": [...], "totalElements": N, "totalPages": N, "last": true/false, ... }
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @PageableDefault(size = 50, sort = "transactionTimestamp", direction = Sort.Direction.DESC) Pageable pageable,
            // ── Filter stubs: declared now so the URL contract is stable; wiring deferred ──
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> type,
            @RequestParam Optional<Integer> accountId,
            @RequestParam Optional<Integer> payeeId,
            @RequestParam Optional<String> fromDate,
            @RequestParam Optional<String> toDate,
            @RequestParam Optional<String> minAmount,
            @RequestParam Optional<String> maxAmount) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
