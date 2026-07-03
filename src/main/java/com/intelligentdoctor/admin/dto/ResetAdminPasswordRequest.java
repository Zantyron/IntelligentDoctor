package com.intelligentdoctor.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetAdminPasswordRequest(
        @NotBlank(message = "password 不能为空")
        @Size(min = 8, max = 128, message = "password 长度应为 8 到 128 个字符")
        String password
) {
}
