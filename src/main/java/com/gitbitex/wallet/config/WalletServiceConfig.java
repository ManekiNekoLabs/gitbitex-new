package com.gitbitex.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.Data;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties({WalletServiceConfig.BitcoinConfig.class})
public class WalletServiceConfig {

    @Bean
    @Profile("!test")
    public boolean walletServiceEnabled() {
        return true;
    }

    @Data
    @ConfigurationProperties(prefix = "wallet.bitcoin")
    public static class BitcoinConfig {
        private String rpcUrl;
        private String rpcUsername;
        private String rpcPassword;
        private String walletPassphrase;
        private int minConfirmations = 3;
        private String coldWalletAddress;
        private boolean enabled = true;
        private int networkType = 0; // 0 for MainNet, 1 for TestNet, 2 for RegTest
    }
} 