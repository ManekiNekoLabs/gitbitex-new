package com.gitbitex.wallet.blockchain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for blockchain operations. Each cryptocurrency implementation should implement this interface.
 */
public interface BlockchainService {
    
    /**
     * Get the currency code this service handles (e.g., "BTC", "ETH")
     */
    String getCurrencyCode();
    
    /**
     * Generate a new address for a user
     * 
     * @param userId The user ID to generate the address for
     * @return The generated address
     */
    String generateAddress(String userId);
    
    /**
     * Validate if an address is valid for this blockchain
     * 
     * @param address The address to validate
     * @return true if the address is valid, false otherwise
     */
    boolean isValidAddress(String address);
    
    /**
     * Get the balance of an address
     * 
     * @param address The address to check
     * @return The balance
     */
    BigDecimal getAddressBalance(String address);
    
    /**
     * Get transaction details
     * 
     * @param txId The transaction ID
     * @return Transaction details as a BlockchainTransaction
     */
    BlockchainTransaction getTransaction(String txId);
    
    /**
     * Get the number of confirmations for a transaction
     * 
     * @param txId The transaction ID
     * @return The number of confirmations
     */
    int getTransactionConfirmations(String txId);
    
    /**
     * Send cryptocurrency to an address
     * 
     * @param toAddress The destination address
     * @param amount The amount to send
     * @param fee The fee to pay (can be null for automatic fee)
     * @return The transaction ID
     */
    String sendToAddress(String toAddress, BigDecimal amount, BigDecimal fee);
    
    /**
     * Scan for new deposits
     * 
     * @return List of new deposits found
     */
    List<BlockchainTransaction> scanForDeposits();
    
    /**
     * Check the status of pending withdrawals
     * 
     * @param pendingTxIds List of pending transaction IDs
     * @return List of updated transaction statuses
     */
    List<BlockchainTransaction> checkWithdrawalStatus(List<String> pendingTxIds);
    
    /**
     * Get the minimum confirmations required for a deposit to be considered final
     * 
     * @return The minimum number of confirmations
     */
    int getMinConfirmations();
} 