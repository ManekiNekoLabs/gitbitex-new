package com.gitbitex.wallet.blockchain.mock;

import com.gitbitex.wallet.blockchain.BlockchainService;
import com.gitbitex.wallet.blockchain.BlockchainTransaction;
import com.gitbitex.wallet.blockchain.BlockchainTransaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mock implementation of the BlockchainService interface for Bitcoin.
 * This is useful for development and testing when a real Bitcoin node is not available.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "wallet.bitcoin.enabled", havingValue = "false")
public class MockBitcoinService implements BlockchainService {
    
    private final Map<String, String> addressesByUser = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> balancesByAddress = new ConcurrentHashMap<>();
    private final Map<String, BlockchainTransaction> transactions = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    public MockBitcoinService() {
        logger.info("Initializing Mock Bitcoin Service");
    }

    @Override
    public String getCurrencyCode() {
        return "BTC";
    }

    @Override
    public String generateAddress(String userId) {
        String address = "mock" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        addressesByUser.put(userId, address);
        balancesByAddress.put(address, BigDecimal.ZERO);
        logger.info("Generated mock address {} for user {}", address, userId);
        return address;
    }

    @Override
    public boolean isValidAddress(String address) {
        return address != null && address.startsWith("mock");
    }

    @Override
    public BigDecimal getAddressBalance(String address) {
        return balancesByAddress.getOrDefault(address, BigDecimal.ZERO);
    }

    @Override
    public BlockchainTransaction getTransaction(String txId) {
        return transactions.get(txId);
    }

    @Override
    public int getTransactionConfirmations(String txId) {
        BlockchainTransaction tx = transactions.get(txId);
        return tx != null ? tx.getConfirmations() : 0;
    }

    @Override
    public String sendToAddress(String toAddress, BigDecimal amount, BigDecimal fee) {
        String txId = "mocktx" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        
        BlockchainTransaction tx = BlockchainTransaction.builder()
                .txId(txId)
                .currency("BTC")
                .toAddress(toAddress)
                .amount(amount)
                .fee(fee != null ? fee : new BigDecimal("0.0001"))
                .confirmations(0)
                .status(TransactionStatus.PENDING)
                .timestamp(Instant.now())
                .build();
        
        transactions.put(txId, tx);
        
        // Simulate transaction confirmation after a delay
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                tx.setConfirmations(6);
                tx.setStatus(TransactionStatus.CONFIRMED);
                logger.info("Mock transaction {} confirmed", txId);
            }
        }, 5000);
        
        logger.info("Sent mock transaction {} to address {}", txId, toAddress);
        return txId;
    }

    @Override
    public List<BlockchainTransaction> scanForDeposits() {
        // Simulate finding new deposits occasionally
        if (random.nextInt(10) < 3) {
            String address = "mock" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            String txId = "mocktx" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            BigDecimal amount = new BigDecimal(random.nextDouble() * 0.1).setScale(8, BigDecimal.ROUND_HALF_UP);
            
            BlockchainTransaction tx = BlockchainTransaction.builder()
                    .txId(txId)
                    .currency("BTC")
                    .toAddress(address)
                    .amount(amount)
                    .confirmations(6)
                    .status(TransactionStatus.CONFIRMED)
                    .timestamp(Instant.now())
                    .build();
            
            transactions.put(txId, tx);
            balancesByAddress.put(address, amount);
            
            logger.info("Found mock deposit {} to address {}", txId, address);
            return Collections.singletonList(tx);
        }
        
        return Collections.emptyList();
    }

    @Override
    public List<BlockchainTransaction> checkWithdrawalStatus(List<String> pendingTxIds) {
        List<BlockchainTransaction> result = new ArrayList<>();
        for (String txId : pendingTxIds) {
            BlockchainTransaction tx = transactions.get(txId);
            if (tx != null) {
                result.add(tx);
            }
        }
        return result;
    }

    @Override
    public int getMinConfirmations() {
        return 6;
    }
} 