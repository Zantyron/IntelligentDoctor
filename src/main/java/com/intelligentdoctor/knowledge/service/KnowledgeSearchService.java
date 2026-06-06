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
import java.util.List;
import java.util.Map;
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
        return knowledgeVectorStore.search(hospitalId, query, limit);
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
}
