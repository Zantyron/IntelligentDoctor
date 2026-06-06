package com.intelligentdoctor.ai.dto;

public record KnowledgeSnippet(
        String id,
        String sourceName,
        String text,
        double score
) {
}
