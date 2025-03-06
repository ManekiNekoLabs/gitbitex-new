package com.gitbitex.wallet.blockchain.bitcoin;

import com.gitbitex.wallet.blockchain.BlockchainTransaction;
import com.gitbitex.wallet.blockchain.BlockchainTransaction.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Client for interacting with a Bitcoin node via JSON-RPC.
 * This is a simplified implementation for demonstration purposes.
 */
@Slf4j
public class BitcoinRpcClient {
    private final String rpcUrl;
    private final String authHeader;
    private final OkHttpClient httpClient;

    public BitcoinRpcClient(String rpcUrl, String rpcUsername, String rpcPassword) {
        this.rpcUrl = rpcUrl;
        String auth = rpcUsername + ":" + rpcPassword;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
        this.httpClient = new OkHttpClient.Builder().build();
    }

    /**
     * Get network information from the Bitcoin node
     */
    public String getNetworkInfo() throws IOException {
        JSONObject response = executeRpcCall("getnetworkinfo", new JSONArray());
        return response.toString();
    }

    /**
     * Generate a new Bitcoin address
     */
    public String generateNewAddress() throws IOException {
        JSONObject response = executeRpcCall("getnewaddress", new JSONArray());
        return response.getString("result");
    }

    /**
     * Validate a Bitcoin address
     */
    public boolean validateAddress(String address) throws IOException {
        JSONArray params = new JSONArray().put(address);
        JSONObject response = executeRpcCall("validateaddress", params);
        return response.getJSONObject("result").getBoolean("isvalid");
    }

    /**
     * Get the balance of a Bitcoin address
     */
    public BigDecimal getAddressBalance(String address) throws IOException {
        // Note: Bitcoin Core doesn't directly support getting balance by address
        // This would require additional indexing or using an external service
        // This is a simplified implementation
        JSONArray params = new JSONArray()
                .put(new JSONArray().put(address))
                .put(1); // minconf
        JSONObject response = executeRpcCall("listreceivedbyaddress", params);
        
        JSONArray received = response.getJSONArray("result");
        if (received.length() > 0) {
            return new BigDecimal(received.getJSONObject(0).getString("amount"));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get transaction details
     */
    public BlockchainTransaction getTransaction(String txId) throws IOException {
        JSONArray params = new JSONArray().put(txId);
        JSONObject response = executeRpcCall("gettransaction", params);
        JSONObject result = response.getJSONObject("result");
        
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setTxId(txId);
        tx.setCurrency("BTC");
        tx.setAmount(new BigDecimal(result.getString("amount")));
        tx.setConfirmations(result.getInt("confirmations"));
        tx.setTimestamp(Instant.ofEpochSecond(result.getLong("time")));
        
        if (tx.getConfirmations() >= 1) {
            tx.setStatus(TransactionStatus.CONFIRMED);
        } else {
            tx.setStatus(TransactionStatus.PENDING);
        }
        
        // In a real implementation, we would parse the transaction details to get
        // the from/to addresses and fee
        
        return tx;
    }

    /**
     * Get the number of confirmations for a transaction
     */
    public int getTransactionConfirmations(String txId) throws IOException {
        JSONArray params = new JSONArray().put(txId);
        JSONObject response = executeRpcCall("gettransaction", params);
        return response.getJSONObject("result").getInt("confirmations");
    }

    /**
     * Send Bitcoin to an address
     */
    public String sendToAddress(String toAddress, BigDecimal amount, BigDecimal fee) throws IOException {
        JSONArray params = new JSONArray()
                .put(toAddress)
                .put(amount.toPlainString());
        
        if (fee != null) {
            // In a real implementation, we would set the fee
            // This is a simplified version
        }
        
        JSONObject response = executeRpcCall("sendtoaddress", params);
        return response.getString("result"); // txid
    }

    /**
     * Unlock the wallet for sending transactions
     */
    public void unlockWallet(String passphrase, int timeoutSeconds) throws IOException {
        JSONArray params = new JSONArray()
                .put(passphrase)
                .put(timeoutSeconds);
        executeRpcCall("walletpassphrase", params);
    }

    /**
     * List received transactions with minimum confirmations
     */
    public List<BlockchainTransaction> listReceivedTransactions(int minConfirmations) throws IOException {
        JSONArray params = new JSONArray()
                .put(minConfirmations)
                .put(true); // includeEmpty
        
        JSONObject response = executeRpcCall("listreceivedbyaddress", params);
        JSONArray result = response.getJSONArray("result");
        
        List<BlockchainTransaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < result.length(); i++) {
            JSONObject item = result.getJSONObject(i);
            String address = item.getString("address");
            BigDecimal amount = new BigDecimal(item.getString("amount"));
            
            JSONArray txIds = item.getJSONArray("txids");
            for (int j = 0; j < txIds.length(); j++) {
                String txId = txIds.getString(j);
                BlockchainTransaction tx = getTransaction(txId);
                if (tx != null) {
                    tx.setToAddress(address);
                    tx.setAmount(amount);
                    transactions.add(tx);
                }
            }
        }
        
        return transactions;
    }

    /**
     * Execute a JSON-RPC call to the Bitcoin node
     */
    private JSONObject executeRpcCall(String method, JSONArray params) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "1.0");
        requestBody.put("id", UUID.randomUUID().toString());
        requestBody.put("method", method);
        requestBody.put("params", params);
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), requestBody.toString());
        
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", authHeader)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            
            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }
} 