package com.gitbitex.wallet.config;

import com.gitbitex.wallet.blockchain.BlockchainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class BlockchainServiceConfig {

    /**
     * Creates a map of blockchain services by currency code.
     * This allows for easy lookup of the appropriate service by currency.
     */
    @Bean
    public Map<String, BlockchainService> blockchainServices(List<BlockchainService> services) {
        Map<String, BlockchainService> serviceMap = new HashMap<>();
        
        for (BlockchainService service : services) {
            serviceMap.put(service.getCurrencyCode(), service);
        }
        
        return serviceMap;
    }
} 