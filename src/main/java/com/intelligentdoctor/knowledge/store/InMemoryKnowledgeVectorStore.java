package com.intelligentdoctor.knowledge.store;

import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.knowledge.store.dto.VectorDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.vector-store", name = "provider", havingValue = "memory")
public class InMemoryKnowledgeVectorStore implements KnowledgeVectorStore {

    private final Map<String, List<VectorDocument>> storage = new ConcurrentHashMap<>();

    @Override
    public void upsert(String hospitalId, List<VectorDocument> documents) {
        storage.computeIfAbsent(hospitalId, key -> new ArrayList<>());
        List<VectorDocument> existing = storage.get(hospitalId);
        Map<String, VectorDocument> merged = new ConcurrentHashMap<>();
        existing.forEach(document -> merged.put(document.id(), document));
        documents.forEach(document -> merged.put(document.id(), document));
        storage.put(hospitalId, new ArrayList<>(merged.values()));
    }

    @Override
    public List<KnowledgeSnippet> search(String hospitalId, String query, int limit) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return storage.getOrDefault(hospitalId, List.of()).stream()
                .map(document -> new KnowledgeSnippet(
                        document.id(),
                        String.valueOf(document.metadata().getOrDefault("sourceName", "知识库")),
                        document.text(),
                        score(document.text(), normalized)
                ))
                .filter(snippet -> snippet.score() > 0)
                .sorted(Comparator.comparingDouble(KnowledgeSnippet::score).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public void deleteHospital(String hospitalId) {
        storage.remove(hospitalId);
    }

    @Override
    public String providerName() {
        return "memory";
    }

    private double score(String text, String query) {
        String normalizedText = text.toLowerCase(Locale.ROOT);
        double score = 0;
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && normalizedText.contains(token)) {
                score += 1.5;
            }
        }
        for (int i = 0; i < query.length(); i++) {
            String c = query.substring(i, i + 1);
            if (!c.isBlank() && normalizedText.contains(c)) {
                score += 0.1;
            }
        }
        return score;
    }
}
