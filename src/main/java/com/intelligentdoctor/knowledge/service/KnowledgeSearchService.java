package com.intelligentdoctor.knowledge.service;

import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.knowledge.entity.KnowledgeChunkEntity;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import com.intelligentdoctor.knowledge.store.KnowledgeVectorStore;
import com.intelligentdoctor.knowledge.store.dto.VectorDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class KnowledgeSearchService {

    private static final Pattern ASCII_VECTOR_ID = Pattern.compile("^[\\x00-\\x7F]+$");

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeVectorStore knowledgeVectorStore;
    private final JsonUtils jsonUtils;

    public KnowledgeSearchService(KnowledgeChunkRepository knowledgeChunkRepository,
                                  KnowledgeVectorStore knowledgeVectorStore,
                                  JsonUtils jsonUtils) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeVectorStore = knowledgeVectorStore;
        this.jsonUtils = jsonUtils;
    }

    public List<KnowledgeSnippet> search(String hospitalId, String query, int limit) {
        String processedQuery = processQuery(query);
        List<KnowledgeSnippet> coarse = knowledgeVectorStore.search(hospitalId, processedQuery, Math.max(limit * 4, 12));
        return rerank(processedQuery, coarse).stream()
                .limit(limit)
                .toList();
    }

    public Map<String, Object> explainSearch(String query, List<KnowledgeSnippet> snippets) {
        return Map.of(
                "paradigm", "ReAct-style tool-augmented RAG agent",
                "queryProcessing", processQuery(query),
                "coarseRetriever", knowledgeVectorStore.providerName(),
                "coarseTopK", Math.max(snippets.size() * 4, 12),
                "reranker", "lexical-symptom-overlap-v1",
                "finalTopK", snippets.size()
        );
    }

    @Transactional
    public void rebuild(String hospitalId) {
        List<KnowledgeChunkEntity> chunks = knowledgeChunkRepository.findByHospitalId(hospitalId);
        knowledgeVectorStore.deleteHospital(hospitalId);
        knowledgeVectorStore.upsert(hospitalId, chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>(jsonUtils.toMap(chunk.getMetadataJson()));
            metadata.put("sourceType", chunk.getSourceType());
            metadata.put("sourceName", chunk.getSourceName());
            metadata.put("text", chunk.getChunkText());
            metadata.put("hospitalId", chunk.getHospitalId());
            metadata.put("chunkKey", chunk.getChunkKey());
            return new VectorDocument(vectorId(chunk), chunk.getChunkText(), metadata);
        }).toList());
    }

    public String providerName() {
        return knowledgeVectorStore.providerName();
    }

    private String vectorId(KnowledgeChunkEntity chunk) {
        if (hasAsciiText(chunk.getExternalVectorId())) {
            return chunk.getExternalVectorId();
        }
        if (hasAsciiText(chunk.getChunkKey())) {
            return chunk.getChunkKey();
        }
        return chunk.getId();
    }

    private boolean hasAsciiText(String value) {
        return value != null && !value.isBlank() && ASCII_VECTOR_ID.matcher(value).matches();
    }

    private String processQuery(String query) {
        String normalized = query == null ? "" : query
                .replaceAll("[，。！？；、,.!?;]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return "导诊 挂号 科室 症状";
        }
        Set<String> expansions = new LinkedHashSet<>();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "皮肤", "皮疹", "瘙痒", "过敏", "湿疹", "痘")) {
            expansions.add("皮肤科");
            expansions.add("皮疹 瘙痒 过敏 湿疹");
        }
        if (containsAny(lower, "肚子", "腹痛", "腹泻", "恶心", "呕吐", "胃")) {
            expansions.add("消化内科");
            expansions.add("腹痛 腹泻 胃肠");
        }
        if (containsAny(lower, "小孩", "儿童", "宝宝", "孩子")) {
            expansions.add("儿科");
            expansions.add("儿童 发热 咳嗽 腹泻");
        }
        if (containsAny(lower, "鼻", "耳", "喉", "咽", "嗓子")) {
            expansions.add("耳鼻喉科");
            expansions.add("鼻塞 咽痛 耳痛");
        }
        if (containsAny(lower, "骨", "关节", "扭伤", "腰痛", "颈椎")) {
            expansions.add("骨科");
            expansions.add("关节 疼痛 扭伤");
        }
        return expansions.isEmpty() ? normalized : normalized + " " + String.join(" ", expansions);
    }

    private List<KnowledgeSnippet> rerank(String query, List<KnowledgeSnippet> candidates) {
        Set<String> tokens = queryTokens(query);
        return candidates.stream()
                .map(snippet -> new KnowledgeSnippet(
                        snippet.id(),
                        snippet.sourceName(),
                        snippet.text(),
                        snippet.score() + lexicalBoost(tokens, snippet.text())
                ))
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .toList();
    }

    private Set<String> queryTokens(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : query.split("\\s+")) {
            if (token.length() >= 2) {
                tokens.add(token.toLowerCase(Locale.ROOT));
            }
        }
        for (int i = 0; i + 2 <= query.length(); i++) {
            String token = query.substring(i, i + 2).trim();
            if (token.length() == 2) {
                tokens.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }

    private double lexicalBoost(Set<String> tokens, String text) {
        String normalizedText = text.toLowerCase(Locale.ROOT);
        double boost = 0;
        for (String token : tokens) {
            if (normalizedText.contains(token)) {
                boost += token.length() > 2 ? 0.35 : 0.12;
            }
        }
        return boost;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
