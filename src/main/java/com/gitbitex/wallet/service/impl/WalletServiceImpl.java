package com.gitbitex.wallet.service.impl;

import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandProducer;
import com.gitbitex.matchingengine.command.WithdrawalCommand;
import com.gitbitex.wallet.blockchain.BlockchainService;
import com.gitbitex.wallet.blockchain.BlockchainTransaction;
import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.Deposit.DepositStatus;
import com.gitbitex.wallet.model.WalletAddress;
import com.gitbitex.wallet.model.Withdrawal;
import com.gitbitex.wallet.model.Withdrawal.WithdrawalStatus;
import com.gitbitex.wallet.repository.DepositRepository;
import com.gitbitex.wallet.repository.WalletAddressRepository;
import com.gitbitex.wallet.repository.WithdrawalRepository;
import com.gitbitex.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean({WalletAddressRepository.class, DepositRepository.class, WithdrawalRepository.class})
public class WalletServiceImpl implements WalletService {
    private final Map<String, BlockchainService> blockchainServices;
    private final WalletAddressRepository walletAddressRepository;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final MatchingEngineCommandProducer matchingEngineCommandProducer;

    @Override
    public WalletAddress generateAddress(String userId, String currency) {
        BlockchainService blockchainService = getBlockchainService(currency);
        if (blockchainService == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        
        // Check if user already has an unused address
        WalletAddress existingAddress = walletAddressRepository.findUnusedAddressByUserIdAndCurrency(userId, currency);
        if (existingAddress != null) {
            return existingAddress;
        }
        
        // Generate a new address
        String address = blockchainService.generateAddress(userId);
        
        WalletAddress walletAddress = new WalletAddress();
        walletAddress.setId(UUID.randomUUID().toString());
        walletAddress.setUserId(userId);
        walletAddress.setCurrency(currency);
        walletAddress.setAddress(address);
        walletAddress.setUsed(false);
        walletAddress.setCreatedAt(Instant.now());
        walletAddress.setUpdatedAt(Instant.now());
        
        walletAddressRepository.save(walletAddress);
        
        return walletAddress;
    }

    @Override
    public List<WalletAddress> getAddresses(String userId, String currency) {
        try {
            return walletAddressRepository.findByUserIdAndCurrency(userId, currency);
        } catch (Exception e) {
            logger.error("Error getting addresses for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Withdrawal requestWithdrawal(String userId, String currency, String address, BigDecimal amount) {
        BlockchainService blockchainService = getBlockchainService(currency);
        if (blockchainService == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        
        // Validate address
        if (!blockchainService.isValidAddress(address)) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
        
        // Create withdrawal record
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(UUID.randomUUID().toString());
        withdrawal.setUserId(userId);
        withdrawal.setCurrency(currency);
        withdrawal.setAddress(address);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setCreatedAt(Instant.now());
        withdrawal.setUpdatedAt(Instant.now());
        
        withdrawalRepository.save(withdrawal);
        
        // Send withdrawal command to matching engine to hold funds
        WithdrawalCommand command = new WithdrawalCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(amount.negate());  // Negative amount for withdrawal
        command.setWithdrawalId(withdrawal.getId());
        
        matchingEngineCommandProducer.send(command, null);
        
        return withdrawal;
    }

    @Override
    public Withdrawal approveWithdrawal(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId);
        if (withdrawal == null) {
            throw new IllegalArgumentException("Withdrawal not found: " + withdrawalId);
        }
        
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Withdrawal is not in PENDING state: " + withdrawalId);
        }
        
        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setUpdatedAt(Instant.now());
        
        withdrawalRepository.save(withdrawal);
        
        return withdrawal;
    }

    @Override
    public Withdrawal rejectWithdrawal(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId);
        if (withdrawal == null) {
            throw new IllegalArgumentException("Withdrawal not found: " + withdrawalId);
        }
        
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Withdrawal is not in PENDING state: " + withdrawalId);
        }
        
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setUpdatedAt(Instant.now());
        
        withdrawalRepository.save(withdrawal);
        
        // Return funds to user
        WithdrawalCommand command = new WithdrawalCommand();
        command.setUserId(withdrawal.getUserId());
        command.setCurrency(withdrawal.getCurrency());
        command.setAmount(withdrawal.getAmount());  // Positive amount to return funds
        command.setWithdrawalId(withdrawal.getId());
        
        matchingEngineCommandProducer.send(command, null);
        
        return withdrawal;
    }

    @Override
    public List<Deposit> getDeposits(String userId, int page, int size) {
        try {
            return depositRepository.findByUserId(userId, page, size);
        } catch (Exception e) {
            logger.error("Error getting deposits for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Withdrawal> getWithdrawals(String userId, int page, int size) {
        try {
            return withdrawalRepository.findByUserId(userId, page, size);
        } catch (Exception e) {
            logger.error("Error getting withdrawals for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000)  // Run every minute
    public void processPendingDeposits() {
        if (blockchainServices.isEmpty()) {
            logger.debug("No blockchain services available, skipping deposit processing");
            return;
        }
        
        logger.info("Processing pending deposits");
        
        for (BlockchainService blockchainService : blockchainServices.values()) {
            try {
                String currency = blockchainService.getCurrencyCode();
                
                // Scan for new deposits
                List<BlockchainTransaction> transactions = blockchainService.scanForDeposits();
                
                for (BlockchainTransaction tx : transactions) {
                    // Check if this transaction has already been processed
                    Deposit existingDeposit = depositRepository.findByTxId(tx.getTxId());
                    if (existingDeposit != null) {
                        // Update confirmations if needed
                        if (existingDeposit.getConfirmations() != tx.getConfirmations()) {
                            existingDeposit.setConfirmations(tx.getConfirmations());
                            existingDeposit.setUpdatedAt(Instant.now());
                            depositRepository.save(existingDeposit);
                            
                            // If deposit now has enough confirmations, process it
                            if (existingDeposit.getStatus() == DepositStatus.PENDING && 
                                    tx.getConfirmations() >= blockchainService.getMinConfirmations()) {
                                processConfirmedDeposit(existingDeposit);
                            }
                        }
                        continue;
                    }
                    
                    // Find the user who owns this address
                    WalletAddress walletAddress = walletAddressRepository.findByAddress(tx.getToAddress());
                    if (walletAddress == null) {
                        logger.warn("Received deposit to unknown address: {}", tx.getToAddress());
                        continue;
                    }
                    
                    // Create new deposit record
                    Deposit deposit = new Deposit();
                    deposit.setId(UUID.randomUUID().toString());
                    deposit.setUserId(walletAddress.getUserId());
                    deposit.setCurrency(currency);
                    deposit.setAddress(tx.getToAddress());
                    deposit.setTxId(tx.getTxId());
                    deposit.setAmount(tx.getAmount());
                    deposit.setConfirmations(tx.getConfirmations());
                    deposit.setStatus(DepositStatus.PENDING);
                    deposit.setCreatedAt(Instant.now());
                    deposit.setUpdatedAt(Instant.now());
                    
                    depositRepository.save(deposit);
                    
                    // Mark address as used
                    walletAddress.setUsed(true);
                    walletAddress.setUpdatedAt(Instant.now());
                    walletAddressRepository.save(walletAddress);
                    
                    // If deposit already has enough confirmations, process it
                    if (tx.getConfirmations() >= blockchainService.getMinConfirmations()) {
                        processConfirmedDeposit(deposit);
                    }
                }
                
                // Check existing pending deposits for confirmations
                List<Deposit> pendingDeposits = depositRepository.findByCurrencyAndStatus(
                        currency, DepositStatus.PENDING, 1, 100);
                
                for (Deposit deposit : pendingDeposits) {
                    try {
                        int confirmations = blockchainService.getTransactionConfirmations(deposit.getTxId());
                        
                        if (confirmations != deposit.getConfirmations()) {
                            deposit.setConfirmations(confirmations);
                            deposit.setUpdatedAt(Instant.now());
                            depositRepository.save(deposit);
                            
                            if (confirmations >= blockchainService.getMinConfirmations()) {
                                processConfirmedDeposit(deposit);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error checking confirmations for deposit: {}", deposit.getId(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing deposits for currency: {}", 
                        blockchainService.getCurrencyCode(), e);
            }
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000)  // Run every minute
    public void processPendingWithdrawals() {
        if (blockchainServices.isEmpty()) {
            logger.debug("No blockchain services available, skipping withdrawal processing");
            return;
        }
        
        logger.info("Processing pending withdrawals");
        
        // Process approved withdrawals
        List<Withdrawal> approvedWithdrawals = withdrawalRepository.findByStatus(
                WithdrawalStatus.APPROVED, 1, 100);
        
        for (Withdrawal withdrawal : approvedWithdrawals) {
            try {
                BlockchainService blockchainService = getBlockchainService(withdrawal.getCurrency());
                if (blockchainService == null) {
                    logger.error("Unsupported currency for withdrawal: {}", withdrawal.getCurrency());
                    continue;
                }
                
                // Send the withdrawal to the blockchain
                String txId = blockchainService.sendToAddress(
                        withdrawal.getAddress(), withdrawal.getAmount(), null);
                
                withdrawal.setTxId(txId);
                withdrawal.setStatus(WithdrawalStatus.PROCESSING);
                withdrawal.setUpdatedAt(Instant.now());
                
                withdrawalRepository.save(withdrawal);
            } catch (Exception e) {
                logger.error("Error processing withdrawal: {}", withdrawal.getId(), e);
                
                // Mark as failed if there's an error
                withdrawal.setStatus(WithdrawalStatus.FAILED);
                withdrawal.setUpdatedAt(Instant.now());
                withdrawalRepository.save(withdrawal);
                
                // Return funds to user
                WithdrawalCommand command = new WithdrawalCommand();
                command.setUserId(withdrawal.getUserId());
                command.setCurrency(withdrawal.getCurrency());
                command.setAmount(withdrawal.getAmount());  // Positive amount to return funds
                command.setWithdrawalId(withdrawal.getId());
                
                matchingEngineCommandProducer.send(command, null);
            }
        }
        
        // Check status of processing withdrawals
        List<Withdrawal> processingWithdrawals = withdrawalRepository.findByStatus(
                WithdrawalStatus.PROCESSING, 1, 100);
        
        // Group by currency for efficiency
        Map<String, List<Withdrawal>> withdrawalsByCurrency = processingWithdrawals.stream()
                .collect(Collectors.groupingBy(Withdrawal::getCurrency));
        
        for (Map.Entry<String, List<Withdrawal>> entry : withdrawalsByCurrency.entrySet()) {
            String currency = entry.getKey();
            List<Withdrawal> withdrawals = entry.getValue();
            
            BlockchainService blockchainService = getBlockchainService(currency);
            if (blockchainService == null) {
                continue;
            }
            
            // Get all txIds for this currency
            List<String> txIds = withdrawals.stream()
                    .map(Withdrawal::getTxId)
                    .collect(Collectors.toList());
            
            try {
                // Check status of all transactions at once
                List<BlockchainTransaction> txStatuses = blockchainService.checkWithdrawalStatus(txIds);
                
                // Create a map for easy lookup
                Map<String, BlockchainTransaction> txStatusMap = txStatuses.stream()
                        .collect(Collectors.toMap(BlockchainTransaction::getTxId, Function.identity()));
                
                // Update withdrawal statuses
                for (Withdrawal withdrawal : withdrawals) {
                    BlockchainTransaction tx = txStatusMap.get(withdrawal.getTxId());
                    if (tx != null && tx.getStatus() == BlockchainTransaction.TransactionStatus.CONFIRMED) {
                        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
                        withdrawal.setUpdatedAt(Instant.now());
                        withdrawalRepository.save(withdrawal);
                    } else if (tx != null && tx.getStatus() == BlockchainTransaction.TransactionStatus.FAILED) {
                        withdrawal.setStatus(WithdrawalStatus.FAILED);
                        withdrawal.setUpdatedAt(Instant.now());
                        withdrawalRepository.save(withdrawal);
                        
                        // Return funds to user
                        WithdrawalCommand command = new WithdrawalCommand();
                        command.setUserId(withdrawal.getUserId());
                        command.setCurrency(withdrawal.getCurrency());
                        command.setAmount(withdrawal.getAmount());  // Positive amount to return funds
                        command.setWithdrawalId(withdrawal.getId());
                        
                        matchingEngineCommandProducer.send(command, null);
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking withdrawal statuses for currency: {}", currency, e);
            }
        }
    }

    private void processConfirmedDeposit(Deposit deposit) {
        deposit.setStatus(DepositStatus.COMPLETED);
        deposit.setUpdatedAt(Instant.now());
        depositRepository.save(deposit);
        
        // Send deposit command to matching engine
        DepositCommand command = new DepositCommand();
        command.setUserId(deposit.getUserId());
        command.setCurrency(deposit.getCurrency());
        command.setAmount(deposit.getAmount());
        command.setTransactionId(deposit.getId());
        
        matchingEngineCommandProducer.send(command, null);
        
        logger.info("Processed confirmed deposit: {}", deposit.getId());
    }

    private BlockchainService getBlockchainService(String currency) {
        BlockchainService service = blockchainServices.get(currency.toUpperCase());
        if (service == null) {
            logger.warn("No blockchain service available for currency: {}", currency);
        }
        return service;
    }
} 