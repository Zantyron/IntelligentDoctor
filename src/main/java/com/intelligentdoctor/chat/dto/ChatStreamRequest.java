package com.intelligentdoctor.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatStreamRequest(
        @NotBlank(message = "sessionId 不能为空")
        String sessionId,
        String hospitalId,
        @NotEmpty(message = "messages 不能为空")
        List<@Valid ChatMessageInput> messages,
        boolean consentToStoreHistory
) {
}
