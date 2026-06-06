package com.intelligentdoctor.system.service;

import com.intelligentdoctor.config.AppProperties;
import com.mongodb.client.MongoClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.bson.Document;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProviderStatusService {

    private final AppProperties properties;
    private final DataSource dataSource;
    private final MongoClient mongoClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaAdmin kafkaAdmin;

    public ProviderStatusService(AppProperties properties,
                                 DataSource dataSource,
                                 MongoClient mongoClient,
                                 StringRedisTemplate redisTemplate,
                                 KafkaAdmin kafkaAdmin) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.mongoClient = mongoClient;
        this.redisTemplate = redisTemplate;
        this.kafkaAdmin = kafkaAdmin;
    }

    public Map<String, Object> statuses() {
        return Map.of(
                "mysql", mysqlStatus(),
                "mongodb", mongoStatus(),
                "redis", "redis".equalsIgnoreCase(properties.getRegistration().getStockProvider())
                        ? redisStatus()
                        : disabledStatus("stock-provider is " + properties.getRegistration().getStockProvider()),
                "kafka", "kafka".equalsIgnoreCase(properties.getRegistration().getEventProvider())
                        ? kafkaStatus()
                        : disabledStatus(properties.getRegistration().getEventProvider()),
                "openai", configuredStatus(
                        properties.getAi().getProvider(),
                        hasText(properties.getAi().resolvedApiKey()),
                        Map.of(
                                "baseUrl", properties.getAi().getBaseUrl(),
                                "chatModel", properties.getAi().getChatModel()
                        )),
                "embedding", configuredStatus(
                        "openai-compatible",
                        hasText(properties.getAi().resolvedEmbeddingApiKey()),
                        Map.of(
                                "baseUrl", properties.getAi().getEmbeddingBaseUrl(),
                                "model", properties.getAi().getEmbeddingModel(),
                                "dimensions", properties.getAi().getEmbeddingDimensions()
                        )),
                "pinecone", configuredStatus(
                        properties.getVectorStore().getProvider(),
                        hasText(properties.getVectorStore().getPinecone().getApiKey())
                                && hasText(properties.getVectorStore().getPinecone().getIndexHost()),
                        Map.of(
                                "indexHost", properties.getVectorStore().getPinecone().getIndexHost(),
                                "namespacePrefix", properties.getVectorStore().getNamespacePrefix(),
                                "apiVersion", properties.getVectorStore().getPinecone().getApiVersion()
                        ))
        );
    }

    private Map<String, Object> mysqlStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return up(Map.of(
                    "database", connection.getCatalog(),
                    "url", connection.getMetaData().getURL()
            ));
        } catch (Exception ex) {
            return down(ex);
        }
    }

    private Map<String, Object> mongoStatus() {
        try {
            Document result = mongoClient.getDatabase("admin")
                    .runCommand(new Document("ping", 1));
            return up(Map.of("ok", result.get("ok")));
        } catch (Exception ex) {
            return down(ex);
        }
    }

    private Map<String, Object> redisStatus() {
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            return up(Map.of("response", pong));
        } catch (Exception ex) {
            return down(ex);
        }
    }

    private Map<String, Object> kafkaStatus() {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put("request.timeout.ms", "3000");
        config.put("default.api.timeout.ms", "3000");
        try (AdminClient adminClient = AdminClient.create(config)) {
            String clusterId = adminClient.describeCluster()
                    .clusterId()
                    .get(Duration.ofSeconds(3).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return up(Map.of(
                    "clusterId", clusterId == null ? "" : clusterId,
                    "topic", properties.getRegistration().getKafkaTopic()
            ));
        } catch (Exception ex) {
            return down(ex);
        }
    }

    private Map<String, Object> configuredStatus(String provider, boolean configured, Map<String, Object> details) {
        return Map.of(
                "provider", provider,
                "status", configured ? "configured" : "missing-config",
                "details", details
        );
    }

    private Map<String, Object> disabledStatus(String activeProvider) {
        return Map.of(
                "status", "disabled",
                "reason", "event-provider is " + activeProvider
        );
    }

    private Map<String, Object> up(Map<String, Object> details) {
        return Map.of("status", "up", "details", details);
    }

    private Map<String, Object> down(Exception ex) {
        return Map.of(
                "status", "down",
                "error", ex.getClass().getSimpleName() + ": " + ex.getMessage()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
