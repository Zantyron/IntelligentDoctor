package com.intelligentdoctor.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageInput(
        @NotBlank(message = "role 不能为空")
        String role,
        @NotBlank(message = "content 不能为空")
        String content
) {
}
