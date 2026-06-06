package com.intelligentdoctor.knowledge.store;

import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.knowledge.store.dto.VectorDocument;

import java.util.List;

public interface KnowledgeVectorStore {

    void upsert(String hospitalId, List<VectorDocument> documents);

    List<KnowledgeSnippet> search(String hospitalId, String query, int limit);

    void deleteHospital(String hospitalId);

    String providerName();
}
