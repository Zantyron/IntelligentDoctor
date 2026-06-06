package com.intelligentdoctor.ai.dto;

import com.intelligentdoctor.chat.model.ChatMode;

import java.util.List;

public record AiPromptContext(
        ChatMode mode,
        String systemPrompt,
        String businessPrompt,
        String ragPrompt,
        String toolPrompt,
        List<String> evidence
) {
}
