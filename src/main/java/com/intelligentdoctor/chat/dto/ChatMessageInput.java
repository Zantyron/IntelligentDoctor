package com.intelligentdoctor.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageInput(
        @NotBlank(message = "role 不能为空")
        @Pattern(regexp = "user|assistant|system", message = "role 只能是 user、assistant 或 system")
        String role,
        @NotBlank(message = "content 不能为空")
        @Size(max = 4000, message = "content 不能超过 4000 个字符")
        String content
) {
}
