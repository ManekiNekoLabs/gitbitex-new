package com.gitbitex.wallet.service.impl;

import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.WalletAddress;
import com.gitbitex.wallet.model.Withdrawal;
import com.gitbitex.wallet.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * A no-op implementation of the WalletService interface.
 * This is used when the required beans for the real implementation are not available.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(WalletServiceImpl.class)
public class NoOpWalletService implements WalletService {

    public NoOpWalletService() {
        logger.info("Initializing NoOpWalletService - wallet functionality will be disabled");
    }

    @Override
    public WalletAddress generateAddress(String userId, String currency) {
        logger.warn("Wallet functionality is disabled - generateAddress called for user {} and currency {}", userId, currency);
        return null;
    }

    @Override
    public List<WalletAddress> getAddresses(String userId, String currency) {
        logger.debug("Wallet functionality is disabled - getAddresses called for user {} and currency {}", userId, currency);
        return Collections.emptyList();
    }

    @Override
    public Withdrawal requestWithdrawal(String userId, String currency, String address, BigDecimal amount) {
        logger.warn("Wallet functionality is disabled - requestWithdrawal called for user {} and currency {}", userId, currency);
        return null;
    }

    @Override
    public Withdrawal approveWithdrawal(String withdrawalId) {
        logger.warn("Wallet functionality is disabled - approveWithdrawal called for withdrawal {}", withdrawalId);
        return null;
    }

    @Override
    public Withdrawal rejectWithdrawal(String withdrawalId) {
        logger.warn("Wallet functionality is disabled - rejectWithdrawal called for withdrawal {}", withdrawalId);
        return null;
    }

    @Override
    public List<Deposit> getDeposits(String userId, int page, int size) {
        logger.debug("Wallet functionality is disabled - getDeposits called for user {}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<Withdrawal> getWithdrawals(String userId, int page, int size) {
        logger.debug("Wallet functionality is disabled - getWithdrawals called for user {}", userId);
        return Collections.emptyList();
    }

    @Override
    public void processPendingDeposits() {
        // Do nothing
    }

    @Override
    public void processPendingWithdrawals() {
        // Do nothing
    }
} 