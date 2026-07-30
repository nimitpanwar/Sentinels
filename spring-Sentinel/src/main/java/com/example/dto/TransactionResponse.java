package com.example.dto;

import com.example.enums.TransactionSource;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private Instant timestamp;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;
    private TransactionSource source;
}
