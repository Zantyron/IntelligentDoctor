package com.intelligentdoctor.common;

import java.util.Map;

public record SsePayload(
        String type,
        String content,
        Map<String, Object> metadata
) {
    public static SsePayload of(String type, String content) {
        return new SsePayload(type, content, Map.of());
    }

    public static SsePayload of(String type, String content, Map<String, Object> metadata) {
        return new SsePayload(type, content, metadata);
    }
}
