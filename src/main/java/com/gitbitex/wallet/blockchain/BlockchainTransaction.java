package com.gitbitex.wallet.blockchain;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a blockchain transaction with common fields across different cryptocurrencies.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainTransaction {
    private String txId;
    private String currency;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private BigDecimal fee;
    private int confirmations;
    private TransactionStatus status;
    private Instant timestamp;
    
    /**
     * Possible statuses for a blockchain transaction
     */
    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        UNKNOWN
    }
} 