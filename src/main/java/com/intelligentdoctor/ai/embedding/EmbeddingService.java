package com.intelligentdoctor.ai.embedding;

import com.intelligentdoctor.config.AppProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class EmbeddingService {

    private final AppProperties properties;
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(AppProperties properties) {
        this.properties = properties;
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(properties.getAi().getEmbeddingBaseUrl())
                .apiKey(properties.getAi().resolvedEmbeddingApiKey())
                .modelName(properties.getAi().getEmbeddingModel())
                .dimensions(properties.getAi().getEmbeddingDimensions())
                .timeout(Duration.ofSeconds(45))
                .maxRetries(2)
                .build();
    }

    public List<Double> embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vectorAsList().stream()
                .map(Float::doubleValue)
                .toList();
    }

    public boolean isConfigured() {
        return hasText(properties.getAi().getEmbeddingBaseUrl())
                && hasText(properties.getAi().resolvedEmbeddingApiKey())
                && hasText(properties.getAi().getEmbeddingModel());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
