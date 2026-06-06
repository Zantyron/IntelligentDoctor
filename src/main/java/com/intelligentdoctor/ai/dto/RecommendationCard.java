package com.intelligentdoctor.ai.dto;

public record RecommendationCard(
        String type,
        String id,
        String title,
        String subtitle,
        String description,
        String reason
) {
}
