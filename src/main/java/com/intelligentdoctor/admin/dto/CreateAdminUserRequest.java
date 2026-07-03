package com.intelligentdoctor.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank(message = "username 不能为空")
        @Size(min = 3, max = 64, message = "username 长度应为 3 到 64 个字符")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username 只能包含字母、数字、点、下划线和短横线")
        String username,
        @NotBlank(message = "password 不能为空")
        @Size(min = 8, max = 128, message = "password 长度应为 8 到 128 个字符")
        String password,
        Boolean enabled
) {
}
