package com.gitbitex.wallet.dto;

import lombok.Data;

@Data
public class AddressDto {
    private String id;
    private String currency;
    private String address;
    private String createdAt;
} 