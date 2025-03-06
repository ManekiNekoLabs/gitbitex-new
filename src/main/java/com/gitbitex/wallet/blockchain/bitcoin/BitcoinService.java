package com.gitbitex.wallet.blockchain.bitcoin;

import com.gitbitex.wallet.blockchain.BlockchainService;
import com.gitbitex.wallet.blockchain.BlockchainTransaction;
import com.gitbitex.wallet.config.WalletServiceConfig.BitcoinConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitcoin implementation of the BlockchainService interface.
 * This is a placeholder implementation that would need to be connected to a real Bitcoin node.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "wallet.bitcoin.enabled", havingValue = "true")
public class BitcoinService implements BlockchainService {
    private final BitcoinConfig config;
    private final BitcoinRpcClient rpcClient;

    public BitcoinService(BitcoinConfig config) {
        this.config = config;
        this.rpcClient = new BitcoinRpcClient(
                config.getRpcUrl(),
                config.getRpcUsername(),
                config.getRpcPassword());
        
        // Initialize connection to Bitcoin node
        try {
            String networkInfo = rpcClient.getNetworkInfo();
            logger.info("Connected to Bitcoin node: {}", networkInfo);
        } catch (Exception e) {
            logger.warn("Failed to connect to Bitcoin node: {}. Service will be available but non-functional.", e.getMessage());
            // Don't rethrow the exception to allow the application to start
        }
    }

    @Override
    public String getCurrencyCode() {
        return "BTC";
    }

    @Override
    public String generateAddress(String userId) {
        try {
            // In a real implementation, this would call the Bitcoin node to generate a new address
            return rpcClient.generateNewAddress();
        } catch (Exception e) {
            logger.error("Failed to generate Bitcoin address for user: {}", userId, e);
            throw new RuntimeException("Failed to generate Bitcoin address", e);
        }
    }

    @Override
    public boolean isValidAddress(String address) {
        try {
            return rpcClient.validateAddress(address);
        } catch (Exception e) {
            logger.error("Failed to validate Bitcoin address: {}", address, e);
            return false;
        }
    }

    @Override
    public BigDecimal getAddressBalance(String address) {
        try {
            return rpcClient.getAddressBalance(address);
        } catch (Exception e) {
            logger.error("Failed to get balance for address: {}", address, e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BlockchainTransaction getTransaction(String txId) {
        try {
            return rpcClient.getTransaction(txId);
        } catch (Exception e) {
            logger.error("Failed to get transaction: {}", txId, e);
            return null;
        }
    }

    @Override
    public int getTransactionConfirmations(String txId) {
        try {
            return rpcClient.getTransactionConfirmations(txId);
        } catch (Exception e) {
            logger.error("Failed to get confirmations for transaction: {}", txId, e);
            return 0;
        }
    }

    @Override
    public String sendToAddress(String toAddress, BigDecimal amount, BigDecimal fee) {
        try {
            // In a real implementation, this would call the Bitcoin node to send funds
            if (config.getWalletPassphrase() != null && !config.getWalletPassphrase().isEmpty()) {
                rpcClient.unlockWallet(config.getWalletPassphrase(), 30);
            }
            
            return rpcClient.sendToAddress(toAddress, amount, fee);
        } catch (Exception e) {
            logger.error("Failed to send Bitcoin to address: {}", toAddress, e);
            throw new RuntimeException("Failed to send Bitcoin", e);
        }
    }

    @Override
    public List<BlockchainTransaction> scanForDeposits() {
        try {
            // In a real implementation, this would scan the blockchain for new deposits
            return rpcClient.listReceivedTransactions(config.getMinConfirmations());
        } catch (Exception e) {
            logger.error("Failed to scan for Bitcoin deposits", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<BlockchainTransaction> checkWithdrawalStatus(List<String> pendingTxIds) {
        List<BlockchainTransaction> updatedTransactions = new ArrayList<>();
        
        for (String txId : pendingTxIds) {
            try {
                BlockchainTransaction tx = getTransaction(txId);
                if (tx != null) {
                    updatedTransactions.add(tx);
                }
            } catch (Exception e) {
                logger.error("Failed to check status for transaction: {}", txId, e);
            }
        }
        
        return updatedTransactions;
    }

    @Override
    public int getMinConfirmations() {
        return config.getMinConfirmations();
    }
} 