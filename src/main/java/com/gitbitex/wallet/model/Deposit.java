package com.gitbitex.wallet.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Deposit {
    private String id;
    private String userId;
    private String currency;
    private String address;
    private String txId;
    private BigDecimal amount;
    private int confirmations;
    private DepositStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    public enum DepositStatus {
        PENDING,
        COMPLETED,
        REJECTED
    }
} 