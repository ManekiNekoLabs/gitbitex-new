package com.gitbitex.wallet.repository;

import com.gitbitex.wallet.model.WalletAddress;

import java.util.List;

public interface WalletAddressRepository {
    WalletAddress findById(String id);
    WalletAddress findByAddress(String address);
    List<WalletAddress> findByUserIdAndCurrency(String userId, String currency);
    WalletAddress findUnusedAddressByUserIdAndCurrency(String userId, String currency);
    void save(WalletAddress walletAddress);
} 