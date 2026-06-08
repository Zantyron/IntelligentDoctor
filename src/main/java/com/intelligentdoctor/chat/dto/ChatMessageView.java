package com.intelligentdoctor.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageView(
        String id,
        String sessionId,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
