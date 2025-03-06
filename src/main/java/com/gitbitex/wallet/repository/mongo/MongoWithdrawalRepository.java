package com.gitbitex.wallet.repository.mongo;

import com.gitbitex.wallet.model.Withdrawal;
import com.gitbitex.wallet.model.Withdrawal.WithdrawalStatus;
import com.gitbitex.wallet.repository.WithdrawalRepository;
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
public class MongoWithdrawalRepository implements WithdrawalRepository {
    private final MongoDatabase mongoDatabase;

    @Override
    public Withdrawal findById(String id) {
        Document document = getCollection().find(Filters.eq("_id", id)).first();
        return document != null ? documentToWithdrawal(document) : null;
    }

    @Override
    public Withdrawal findByTxId(String txId) {
        Document document = getCollection().find(Filters.eq("txId", txId)).first();
        return document != null ? documentToWithdrawal(document) : null;
    }

    @Override
    public List<Withdrawal> findByUserId(String userId, int page, int size) {
        List<Withdrawal> withdrawals = new ArrayList<>();
        getCollection().find(Filters.eq("userId", userId))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> withdrawals.add(documentToWithdrawal(document)));
        return withdrawals;
    }

    @Override
    public List<Withdrawal> findByStatus(WithdrawalStatus status, int page, int size) {
        List<Withdrawal> withdrawals = new ArrayList<>();
        getCollection().find(Filters.eq("status", status.name()))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> withdrawals.add(documentToWithdrawal(document)));
        return withdrawals;
    }

    @Override
    public List<Withdrawal> findByCurrencyAndStatus(String currency, WithdrawalStatus status, int page, int size) {
        List<Withdrawal> withdrawals = new ArrayList<>();
        getCollection().find(Filters.and(
                        Filters.eq("currency", currency),
                        Filters.eq("status", status.name())))
                .sort(new Document("createdAt", -1))
                .skip((page - 1) * size)
                .limit(size)
                .forEach(document -> withdrawals.add(documentToWithdrawal(document)));
        return withdrawals;
    }

    @Override
    public void save(Withdrawal withdrawal) {
        if (withdrawal.getId() == null) {
            withdrawal.setId(UUID.randomUUID().toString());
        }
        
        if (withdrawal.getCreatedAt() == null) {
            withdrawal.setCreatedAt(Instant.now());
        }
        
        withdrawal.setUpdatedAt(Instant.now());
        
        Document document = new Document();
        document.put("_id", withdrawal.getId());
        document.put("userId", withdrawal.getUserId());
        document.put("currency", withdrawal.getCurrency());
        document.put("address", withdrawal.getAddress());
        document.put("txId", withdrawal.getTxId());
        document.put("amount", withdrawal.getAmount());
        document.put("fee", withdrawal.getFee());
        document.put("status", withdrawal.getStatus().name());
        document.put("createdAt", withdrawal.getCreatedAt());
        document.put("updatedAt", withdrawal.getUpdatedAt());
        
        getCollection().replaceOne(
                Filters.eq("_id", withdrawal.getId()),
                document,
                new ReplaceOptions().upsert(true));
    }

    private MongoCollection<Document> getCollection() {
        return mongoDatabase.getCollection("withdrawals");
    }

    private Withdrawal documentToWithdrawal(Document document) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(document.getString("_id"));
        withdrawal.setUserId(document.getString("userId"));
        withdrawal.setCurrency(document.getString("currency"));
        withdrawal.setAddress(document.getString("address"));
        withdrawal.setTxId(document.getString("txId"));
        withdrawal.setAmount(document.get("amount", java.math.BigDecimal.class));
        withdrawal.setFee(document.get("fee", java.math.BigDecimal.class));
        withdrawal.setStatus(WithdrawalStatus.valueOf(document.getString("status")));
        withdrawal.setCreatedAt(document.get("createdAt", Instant.class));
        withdrawal.setUpdatedAt(document.get("updatedAt", Instant.class));
        return withdrawal;
    }
} 