package com.intelligentdoctor.chat.dto;

import java.time.LocalDateTime;

public record ChatSessionView(
        String sessionId,
        String hospitalId,
        String mode,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
