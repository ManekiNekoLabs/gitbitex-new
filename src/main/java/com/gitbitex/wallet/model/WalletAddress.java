package com.gitbitex.wallet.model;

import lombok.Data;

import java.time.Instant;

@Data
public class WalletAddress {
    private String id;
    private String userId;
    private String currency;
    private String address;
    private boolean used;
    private Instant createdAt;
    private Instant updatedAt;
} 