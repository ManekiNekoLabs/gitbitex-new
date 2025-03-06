package com.gitbitex.middleware.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MongoProperties.class)
@Slf4j
public class MongoDbConfig {

    private final MongoProperties mongoProperties;

    @Value("${mongodb.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${mongodb.max-wait-time:120000}")
    private int maxWaitTime;

    @Value("${mongodb.retry-writes:true}")
    private boolean retryWrites;

    @Value("${mongodb.retry-reads:true}")
    private boolean retryReads;

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        try {
            ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());

            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSocketSettings(builder ->
                        builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .applyToConnectionPoolSettings(builder ->
                        builder.maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS)
                               .maxSize(50)
                               .minSize(5))
                    .retryWrites(retryWrites)
                    .retryReads(retryReads)
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            logger.info("Connecting to MongoDB at {}", connectionString.getHosts());
            return MongoClients.create(settings);
        } catch (Exception e) {
            logger.error("Failed to create MongoDB client: {}", e.getMessage(), e);

            // Create a fallback client that connects to localhost for development
            logger.warn("Creating fallback MongoDB client connecting to localhost:27017");

            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder ->
                        builder.hosts(Arrays.asList(new ServerAddress("localhost", 27017))))
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            return MongoClients.create(settings);
        }
    }

    @Bean
    public MongoDatabase database(MongoClient mongoClient) {
        try {
            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            return mongoClient.getDatabase("gitbitex").withCodecRegistry(pojoCodecRegistry);
        } catch (Exception e) {
            logger.error("Failed to get MongoDB database: {}", e.getMessage(), e);
            throw e;
        }
    }
}



