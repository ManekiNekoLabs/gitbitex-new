package com.gitbitex.wallet.repository.mongo;

import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.Deposit.DepositStatus;
import com.gitbitex.wallet.repository.DepositRepository;
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
public class MongoDepositRepository implements DepositRepository {
    private final MongoDatabase mongoDatabase;

    @Override
    public Deposit findById(String id) {
        Document document = getCollection().find(Filters.eq("_id", id)).first();
        return document != null ? documentToDeposit(document) : null;
    }

    @Override
    public Deposit findByTxId(String txId) {
        Document document = getCollection().find(Filters.eq("txId", txId)).first();
        return document != null ? documentToDeposit(document) : null;
    }

    @Override
    public List<Deposit> findByUserId(String userId, int page, int size) {
        List<Deposit> deposits = new ArrayList<>();
        getCollection().find(Filters.eq("userId", userId))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> deposits.add(documentToDeposit(document)));
        return deposits;
    }

    @Override
    public List<Deposit> findByStatus(DepositStatus status, int page, int size) {
        List<Deposit> deposits = new ArrayList<>();
        getCollection().find(Filters.eq("status", status.name()))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> deposits.add(documentToDeposit(document)));
        return deposits;
    }

    @Override
    public List<Deposit> findByCurrencyAndStatus(String currency, DepositStatus status, int page, int size) {
        List<Deposit> deposits = new ArrayList<>();
        getCollection().find(Filters.and(
                        Filters.eq("currency", currency),
                        Filters.eq("status", status.name())))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> deposits.add(documentToDeposit(document)));
        return deposits;
    }

    @Override
    public void save(Deposit deposit) {
        if (deposit.getId() == null) {
            deposit.setId(UUID.randomUUID().toString());
        }
        
        if (deposit.getCreatedAt() == null) {
            deposit.setCreatedAt(Instant.now());
        }
        
        deposit.setUpdatedAt(Instant.now());
        
        Document document = new Document();
        document.put("_id", deposit.getId());
        document.put("userId", deposit.getUserId());
        document.put("currency", deposit.getCurrency());
        document.put("address", deposit.getAddress());
        document.put("txId", deposit.getTxId());
        document.put("amount", deposit.getAmount());
        document.put("confirmations", deposit.getConfirmations());
        document.put("status", deposit.getStatus().name());
        document.put("createdAt", deposit.getCreatedAt());
        document.put("updatedAt", deposit.getUpdatedAt());
        
        getCollection().replaceOne(
                Filters.eq("_id", deposit.getId()),
                document,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public long countByUserIdAndCurrency(String userId, String currency) {
        return getCollection().countDocuments(Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("currency", currency)));
    }

    private MongoCollection<Document> getCollection() {
        return mongoDatabase.getCollection("deposits");
    }

    private Deposit documentToDeposit(Document document) {
        Deposit deposit = new Deposit();
        deposit.setId(document.getString("_id"));
        deposit.setUserId(document.getString("userId"));
        deposit.setCurrency(document.getString("currency"));
        deposit.setAddress(document.getString("address"));
        deposit.setTxId(document.getString("txId"));
        deposit.setAmount(document.get("amount", java.math.BigDecimal.class));
        deposit.setConfirmations(document.getInteger("confirmations", 0));
        deposit.setStatus(DepositStatus.valueOf(document.getString("status")));
        deposit.setCreatedAt(document.get("createdAt", Instant.class));
        deposit.setUpdatedAt(document.get("updatedAt", Instant.class));
        return deposit;
    }
} 