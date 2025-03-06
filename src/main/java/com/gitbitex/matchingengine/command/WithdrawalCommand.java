package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Command for processing withdrawals in the matching engine.
 * Negative amount means withdrawing funds from the user's account.
 * Positive amount means returning funds to the user's account (e.g., after a failed withdrawal).
 */
@Getter
@Setter
public class WithdrawalCommand extends Command {
    private String userId;
    private String currency;
    private BigDecimal amount;
    private String withdrawalId;

    public WithdrawalCommand() {
        this.setType(CommandType.WITHDRAWAL);
    }
} 