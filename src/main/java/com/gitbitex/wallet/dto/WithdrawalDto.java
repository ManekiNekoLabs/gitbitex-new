package com.gitbitex.wallet.dto;

import lombok.Data;

@Data
public class WithdrawalDto {
    private String id;
    private String currency;
    private String address;
    private String amount;
    private String txId;
    private String status;
    private String createdAt;
} 