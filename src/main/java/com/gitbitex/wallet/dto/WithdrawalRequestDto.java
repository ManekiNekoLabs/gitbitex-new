package com.gitbitex.wallet.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class WithdrawalRequestDto {
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
} 