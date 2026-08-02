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
 *   - GET /api/transactions: Retrieve transactions, paginated (page/size/sort)
 *     and optionally filtered (accountId, payeeId, status, type, minAmount,
 *     maxAmount, from, to, search - all optional, combined with AND).
 *     Output: Spring Page<TransactionResponse> ({content, totalElements, ...})
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

import com.example.dto.TransactionFilter;
import com.example.dto.TransactionRequest;
import com.example.dto.TransactionResponse;
import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) Integer payeeId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "transactionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        TransactionFilter filter = new TransactionFilter(accountId, payeeId, status, type, minAmount, maxAmount, from, to, search);
        return ResponseEntity.ok(transactionService.getTransactions(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
