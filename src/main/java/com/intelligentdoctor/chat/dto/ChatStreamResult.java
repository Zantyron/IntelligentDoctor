package com.intelligentdoctor.chat.dto;

import com.intelligentdoctor.ai.dto.FunctionSuggestion;
import com.intelligentdoctor.ai.dto.RecommendationCard;

import java.util.List;
import java.util.Map;

public record ChatStreamResult(
        String reply,
        String summary,
        List<String> possibleConditions,
        List<RecommendationCard> recommendations,
        List<String> evidence,
        List<FunctionSuggestion> functionSuggestions,
        Map<String, Object> metadata
) {
}
