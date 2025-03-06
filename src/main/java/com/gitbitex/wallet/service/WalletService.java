package com.gitbitex.wallet.service;

import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.WalletAddress;
import com.gitbitex.wallet.model.Withdrawal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for wallet operations.
 */
public interface WalletService {
    
    /**
     * Generate a new deposit address for a user
     * 
     * @param userId User ID
     * @param currency Currency code (e.g., "BTC")
     * @return The generated address
     */
    WalletAddress generateAddress(String userId, String currency);
    
    /**
     * Get deposit addresses for a user
     * 
     * @param userId User ID
     * @param currency Currency code
     * @return List of wallet addresses
     */
    List<WalletAddress> getAddresses(String userId, String currency);
    
    /**
     * Process a withdrawal request
     * 
     * @param userId User ID
     * @param currency Currency code
     * @param address Destination address
     * @param amount Amount to withdraw
     * @return The created withdrawal record
     */
    Withdrawal requestWithdrawal(String userId, String currency, String address, BigDecimal amount);
    
    /**
     * Approve a withdrawal request
     * 
     * @param withdrawalId Withdrawal ID
     * @return The updated withdrawal record
     */
    Withdrawal approveWithdrawal(String withdrawalId);
    
    /**
     * Reject a withdrawal request
     * 
     * @param withdrawalId Withdrawal ID
     * @return The updated withdrawal record
     */
    Withdrawal rejectWithdrawal(String withdrawalId);
    
    /**
     * Get deposits for a user
     * 
     * @param userId User ID
     * @param page Page number
     * @param size Page size
     * @return List of deposits
     */
    List<Deposit> getDeposits(String userId, int page, int size);
    
    /**
     * Get withdrawals for a user
     * 
     * @param userId User ID
     * @param page Page number
     * @param size Page size
     * @return List of withdrawals
     */
    List<Withdrawal> getWithdrawals(String userId, int page, int size);
    
    /**
     * Process pending deposits
     * This would typically be called by a scheduled job
     */
    void processPendingDeposits();
    
    /**
     * Process pending withdrawals
     * This would typically be called by a scheduled job
     */
    void processPendingWithdrawals();
} 