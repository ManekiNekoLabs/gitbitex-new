package com.gitbitex.wallet.repository;

import com.gitbitex.wallet.model.Withdrawal;
import com.gitbitex.wallet.model.Withdrawal.WithdrawalStatus;

import java.util.List;

public interface WithdrawalRepository {
    Withdrawal findById(String id);
    Withdrawal findByTxId(String txId);
    List<Withdrawal> findByUserId(String userId, int page, int size);
    List<Withdrawal> findByStatus(WithdrawalStatus status, int page, int size);
    List<Withdrawal> findByCurrencyAndStatus(String currency, WithdrawalStatus status, int page, int size);
    void save(Withdrawal withdrawal);
} 