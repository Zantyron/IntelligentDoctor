package com.intelligentdoctor.knowledge.store;

import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.embedding.EmbeddingService;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.knowledge.store.dto.VectorDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.vector-store", name = "provider", havingValue = "pinecone")
public class PineconeKnowledgeVectorStore implements KnowledgeVectorStore {

    private final RestClient restClient;
    private final AppProperties properties;
    private final EmbeddingService embeddingService;

    public PineconeKnowledgeVectorStore(RestClient.Builder builder,
                                        AppProperties properties,
                                        EmbeddingService embeddingService) {
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.restClient = builder
                .baseUrl(properties.getVectorStore().getPinecone().getIndexHost())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Api-Key", properties.getVectorStore().getPinecone().getApiKey())
                .defaultHeader("X-Pinecone-API-Version", properties.getVectorStore().getPinecone().getApiVersion())
                .build();
    }

    @Override
    public void upsert(String hospitalId, List<VectorDocument> documents) {
        if (!isConfigured()) {
            return;
        }
        List<Map<String, Object>> vectors = documents.stream()
                .map(document -> Map.of(
                        "id", document.id(),
                        "values", embeddingService.embed(document.text()),
                        "metadata", document.metadata().isEmpty() ? Map.of("text", document.text()) : document.metadata()
                ))
                .toList();

        restClient.post()
                .uri("/vectors/upsert")
                .body(Map.of(
                        "namespace", namespace(hospitalId),
                        "vectors", vectors
                ))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<KnowledgeSnippet> search(String hospitalId, String query, int limit) {
        if (!isConfigured()) {
            return List.of();
        }
        Map<?, ?> response = restClient.post()
                .uri("/query")
                .body(Map.of(
                        "namespace", namespace(hospitalId),
                        "topK", limit,
                        "includeMetadata", true,
                        "vector", embeddingService.embed(query)
                ))
                .retrieve()
                .body(Map.class);

        Object matchesObject = response == null ? null : response.get("matches");
        if (!(matchesObject instanceof List<?> matches)) {
            return List.of();
        }

        return matches.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(match -> {
                    Map<?, ?> metadata = match.get("metadata") instanceof Map<?, ?> meta ? meta : Map.of();
                    Object score = match.containsKey("score") ? match.get("score") : 0D;
                    return new KnowledgeSnippet(
                            String.valueOf(match.get("id")),
                            metadata.containsKey("sourceName") ? String.valueOf(metadata.get("sourceName")) : "Pinecone",
                            metadata.containsKey("text") ? String.valueOf(metadata.get("text")) : query,
                            score instanceof Number number ? number.doubleValue() : 0D
                    );
                })
                .toList();
    }

    @Override
    public void deleteHospital(String hospitalId) {
        if (!isConfigured()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/vectors/delete")
                    .body(Map.of(
                            "namespace", namespace(hospitalId),
                            "deleteAll", true
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            // Pinecone returns 404 when the namespace has not been created yet.
            // Rebuild remains idempotent because the following upsert creates it.
        }
    }

    @Override
    public String providerName() {
        return "pinecone";
    }

    private String namespace(String hospitalId) {
        return properties.getVectorStore().getNamespacePrefix() + "-" + hospitalId;
    }

    private boolean isConfigured() {
        return hasText(properties.getVectorStore().getPinecone().getApiKey())
                && hasText(properties.getVectorStore().getPinecone().getIndexHost())
                && embeddingService.isConfigured();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
