package com.gitbitex.wallet.dto;

import lombok.Data;

@Data
public class DepositDto {
    private String id;
    private String currency;
    private String address;
    private String amount;
    private String txId;
    private int confirmations;
    private String status;
    private String createdAt;
} 