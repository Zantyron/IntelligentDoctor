package com.intelligentdoctor.chat.agent;

import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.model.ChatMode;

import java.util.List;

public record TriageAgentRequest(
        ChatMode mode,
        String sessionId,
        String hospitalId,
        boolean consentToStoreHistory,
        List<ChatMessageInput> messages
) {
}
