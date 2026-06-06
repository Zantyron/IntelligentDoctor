package com.intelligentdoctor.knowledge.store.dto;

import java.util.Map;

public record VectorDocument(
        String id,
        String text,
        Map<String, Object> metadata
) {
}
