package com.gitbitex.wallet.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Withdrawal {
    private String id;
    private String userId;
    private String currency;
    private String address;
    private String txId;
    private BigDecimal amount;
    private BigDecimal fee;
    private WithdrawalStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    public enum WithdrawalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PROCESSING,
        COMPLETED,
        FAILED
    }
} 