package com.gitbitex.wallet.repository.mongo;

import com.gitbitex.wallet.model.WalletAddress;
import com.gitbitex.wallet.repository.WalletAddressRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MongoWalletAddressRepository implements WalletAddressRepository {
    private final MongoDatabase mongoDatabase;

    @Override
    public WalletAddress findById(String id) {
        Document document = getCollection().find(Filters.eq("_id", id)).first();
        return document != null ? documentToWalletAddress(document) : null;
    }

    @Override
    public WalletAddress findByAddress(String address) {
        Document document = getCollection().find(Filters.eq("address", address)).first();
        return document != null ? documentToWalletAddress(document) : null;
    }

    @Override
    public List<WalletAddress> findByUserIdAndCurrency(String userId, String currency) {
        List<WalletAddress> addresses = new ArrayList<>();
        getCollection().find(Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("currency", currency)))
                .forEach(document -> addresses.add(documentToWalletAddress(document)));
        return addresses;
    }

    @Override
    public WalletAddress findUnusedAddressByUserIdAndCurrency(String userId, String currency) {
        Document document = getCollection().find(Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("currency", currency),
                Filters.eq("used", false)))
                .first();
        return document != null ? documentToWalletAddress(document) : null;
    }

    @Override
    public void save(WalletAddress walletAddress) {
        if (walletAddress.getId() == null) {
            walletAddress.setId(UUID.randomUUID().toString());
        }
        
        if (walletAddress.getCreatedAt() == null) {
            walletAddress.setCreatedAt(Instant.now());
        }
        
        walletAddress.setUpdatedAt(Instant.now());
        
        Document document = new Document();
        document.put("_id", walletAddress.getId());
        document.put("userId", walletAddress.getUserId());
        document.put("currency", walletAddress.getCurrency());
        document.put("address", walletAddress.getAddress());
        document.put("used", walletAddress.isUsed());
        document.put("createdAt", walletAddress.getCreatedAt());
        document.put("updatedAt", walletAddress.getUpdatedAt());
        
        getCollection().replaceOne(
                Filters.eq("_id", walletAddress.getId()),
                document,
                new ReplaceOptions().upsert(true));
    }

    private MongoCollection<Document> getCollection() {
        return mongoDatabase.getCollection("wallet_addresses");
    }

    private WalletAddress documentToWalletAddress(Document document) {
        WalletAddress walletAddress = new WalletAddress();
        walletAddress.setId(document.getString("_id"));
        walletAddress.setUserId(document.getString("userId"));
        walletAddress.setCurrency(document.getString("currency"));
        walletAddress.setAddress(document.getString("address"));
        walletAddress.setUsed(document.getBoolean("used", false));
        walletAddress.setCreatedAt(document.get("createdAt", Instant.class));
        walletAddress.setUpdatedAt(document.get("updatedAt", Instant.class));
        return walletAddress;
    }
} 