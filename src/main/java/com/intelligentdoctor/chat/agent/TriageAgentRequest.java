package com.intelligentdoctor.chat.agent;

import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.model.ChatMode;

import java.time.ZonedDateTime;
import java.util.List;

public record TriageAgentRequest(
        ChatMode mode,
        String sessionId,
        String hospitalId,
        boolean consentToStoreHistory,
        ZonedDateTime requestTime,
        List<ChatMessageInput> messages
) {
}
