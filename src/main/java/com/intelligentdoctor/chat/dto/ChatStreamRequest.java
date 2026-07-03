package com.intelligentdoctor.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatStreamRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = 128, message = "sessionId 不能超过 128 个字符")
        String sessionId,
        String hospitalId,
        @NotEmpty(message = "messages 不能为空")
        @Size(max = 20, message = "messages 不能超过 20 条")
        List<@Valid ChatMessageInput> messages,
        boolean consentToStoreHistory
) {
}
