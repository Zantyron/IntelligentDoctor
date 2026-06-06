package com.intelligentdoctor.ai.dto;

import java.util.Map;

public record FunctionSuggestion(
        String name,
        String description,
        Map<String, Object> arguments,
        boolean ready
) {
}
