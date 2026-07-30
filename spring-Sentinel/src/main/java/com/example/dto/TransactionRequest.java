package com.example.dto;

import com.example.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private String description;
}
