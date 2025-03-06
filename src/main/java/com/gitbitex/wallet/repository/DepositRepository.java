package com.gitbitex.wallet.repository;

import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.Deposit.DepositStatus;

import java.util.List;

public interface DepositRepository {
    Deposit findById(String id);
    Deposit findByTxId(String txId);
    List<Deposit> findByUserId(String userId, int page, int size);
    List<Deposit> findByStatus(DepositStatus status, int page, int size);
    List<Deposit> findByCurrencyAndStatus(String currency, DepositStatus status, int page, int size);
    void save(Deposit deposit);
    long countByUserIdAndCurrency(String userId, String currency);
} 